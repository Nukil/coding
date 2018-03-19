package netposa.blacklist.rpc

import netposa.realtime.blacklist.server.LoadControlUtils
import netposa.realtime.blacklist.server.FetchMessageThread
import org.apache.commons.cli.Options
import org.apache.commons.cli.OptionGroup
import org.apache.commons.cli.Option
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.PosixParser
import org.apache.thrift.server.TServer
import org.apache.commons.cli.CommandLineParser
import org.apache.thrift.protocol.TProtocolFactory
import org.apache.thrift.protocol.TCompactProtocol
import org.apache.thrift.protocol.TBinaryProtocol
import kafka.utils.Logging
import org.apache.thrift.TProcessor
import org.apache.thrift.transport.TTransportFactory
import org.apache.thrift.transport.TFramedTransport
import java.net.InetSocketAddress
import java.net.InetAddress
import java.util.concurrent.ExecutorService
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.LinkedBlockingQueue
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.apache.thrift.transport.TNonblockingServerSocket
import org.apache.thrift.server.THsHaServer
import org.apache.thrift.server.TNonblockingServer
import org.apache.thrift.transport.TServerSocket
import org.apache.thrift.server.TThreadPoolServer

class BlacklistMetadataServer(val control_utils: LoadControlUtils,
  val fetchThred: FetchMessageThread, val args: Array[String]) extends Logging {

  val handler: ChangeMetadataServiceHandler =
    new ChangeMetadataServiceHandler(control_utils, fetchThred)

  val DEFAULT_LISTEN_PORT: Int = 31050

  def run() {
    import scala.collection.mutable.Map
    val conf = Map[String, String]()
    var server: TServer = null
    val options: Options = getOptions()

    val cmd: CommandLine = parseArguments(options, args)
    if (cmd.hasOption("help")) {
      printUsage()
      System.exit(1)
    }

    // Get address to bind
    var bindAddress: String = null
    if (cmd.hasOption("bind")) {
      bindAddress = cmd.getOptionValue("bind")
    } else {
      bindAddress = "127.0.0.1"
    }
    conf += "fakecar.thrift.info.bindAddress" -> bindAddress

    // Get port to bind to
    var listenPort: Int = 0
    try {
      if (cmd.hasOption("port")) {
        listenPort = Integer.parseInt(cmd.getOptionValue("port"))
      } else {
        listenPort = DEFAULT_LISTEN_PORT
      }
    } catch {
      case e: Throwable => {
        throw new RuntimeException("Could not parse the value provided for the port option", e)
      }
    }

    val nonblocking: Boolean = cmd.hasOption("nonblocking")
    val hsha: Boolean = cmd.hasOption("hsha")
    var implType: String = "threadpool"
    if (nonblocking) {
      implType = "nonblocking"
    } else if (hsha) {
      implType = "hsha"
    }

    conf += "fakecar.server.thrift.server.type" -> implType
    conf += "fakecar.server.thrift.port" -> listenPort.toString

    // Construct correct ProtocolFactory
    val compact: Boolean = cmd.hasOption("compact")
    val protocolFactory: TProtocolFactory = getTProtocolFactory(compact)

    val p = new ChangeMetadataService.Processor(handler)
    conf += "fakecar.server.thrift.compact" -> compact.toString()
    val processor: TProcessor = p
    val framed: Boolean = cmd.hasOption("framed") || nonblocking || hsha
    val transportFactory: TTransportFactory = getTTransportFactory(framed, 2 * 1024 * 1024)

    val inetSocketAddress: InetSocketAddress = bindToPort(bindAddress, listenPort)
    conf += "fakecar.server.thrift.framed" -> framed.toString

    if (nonblocking) {
      server = getTNonBlockingServer(protocolFactory, processor, transportFactory, inetSocketAddress);
    } else if (hsha) {
      server = getTHsHaServer(protocolFactory, processor, transportFactory, inetSocketAddress);
    } else {
      server = getTThreadPoolServer(protocolFactory, processor, transportFactory, inetSocketAddress);
    }

    info("start socket ........")
    val tserver = server
    tserver.serve()
    info("shutdown socket ........")
  }

  def shutdown() {
    try {
      handler.shutdown
    } catch {
      case e: Throwable => warn("shutdown rpc server error => %s".format(e.getMessage()), e)
    }
  }

  private def getTHsHaServer(protocolFactory: TProtocolFactory,
    processor: TProcessor, transportFactory: TTransportFactory,
    inetSocketAddress: InetSocketAddress): TServer = {
    val serverTransport = new TNonblockingServerSocket(inetSocketAddress);
    info("starting FAKECAR HsHA Thrift server on " + inetSocketAddress.toString());
    val serverArgs = new THsHaServer.Args(serverTransport);
    val executorService = createExecutor(serverArgs.getWorkerThreads());
    serverArgs.executorService(executorService);
    serverArgs.processor(processor);
    serverArgs.transportFactory(transportFactory);
    serverArgs.protocolFactory(protocolFactory);
    new THsHaServer(serverArgs);
  }

  private def createExecutor(workerThreads: Int): ExecutorService = {
    val callQueue = new CallQueue(new LinkedBlockingQueue[CallQueue.Call]());
    val tfb = new ThreadFactoryBuilder()
    tfb.setDaemon(true);
    tfb.setNameFormat("thrift2-worker-%d");
    return new ThreadPoolExecutor(workerThreads, workerThreads,
      Long.MaxValue, TimeUnit.SECONDS, callQueue,
      tfb.build());
  }

  private def getTNonBlockingServer(protocolFactory: TProtocolFactory, processor: TProcessor,
    transportFactory: TTransportFactory, inetSocketAddress: InetSocketAddress): TServer = {
    val serverTransport = new TNonblockingServerSocket(inetSocketAddress);
    info("starting fakecar Nonblocking Thrift server on " + inetSocketAddress.toString());
    val serverArgs = new TNonblockingServer.Args(serverTransport);
    serverArgs.processor(processor);
    serverArgs.transportFactory(transportFactory);
    serverArgs.protocolFactory(protocolFactory);
    new TNonblockingServer(serverArgs);
  }

  private def getTThreadPoolServer(protocolFactory: TProtocolFactory, processor: TProcessor,
    transportFactory: TTransportFactory, inetSocketAddress: InetSocketAddress): TServer = {
    val serverTransport = new TServerSocket(inetSocketAddress);
    info("starting fakecar ThreadPool Thrift server on " + inetSocketAddress.toString());
    val serverArgs = new TThreadPoolServer.Args(serverTransport);
    serverArgs.processor(processor);
    serverArgs.transportFactory(transportFactory);
    serverArgs.protocolFactory(protocolFactory);
    new TThreadPoolServer(serverArgs);
  }

  private def bindToPort(bindValue: String, listenPort: Int): InetSocketAddress = {
    try {
      if (bindValue == null) {
        new InetSocketAddress(listenPort)
      } else {
        new InetSocketAddress(InetAddress.getByName(bindValue), listenPort)
      }
    } catch {
      case e: Throwable => {
        throw new RuntimeException("Could not bind to provided ip address", e)
      }
    }
  }

  private def getTTransportFactory(framed: Boolean, frameSize: Int): TTransportFactory = {
    if (framed) {
      info("Using framed transport")
      new TFramedTransport.Factory(frameSize)
    } else {
      new TTransportFactory()
    }
  }

  private def getTProtocolFactory(isCompact: Boolean): TProtocolFactory = {
    if (isCompact) {
      info("Using compact protocol")
      new TCompactProtocol.Factory()
    } else {
      info("Using binary protocol")
      new TBinaryProtocol.Factory()
    }
  }

  private def parseArguments(options: Options, args: Array[String]): CommandLine = {
    val new_parser: CommandLineParser = new PosixParser();
    return new_parser.parse(options, args)
  }

  private def printUsage() = {
    val formatter = new HelpFormatter();
    formatter.printHelp("Thrift", null, getOptions(),
      "To start the Fakecar server run 'bin/start.sh'\n" +
        " send a kill signal to the thrift server pid",
      true);
  }

  private def getOptions(): Options = {
    val options = new Options();
    options.addOption("b", "bind", true,
      "Address to bind the fakecar server to. [default: 0.0.0.0]");
    options.addOption("p", "port", true, "Port to bind to [default: " + DEFAULT_LISTEN_PORT + "]");
    options.addOption("f", "framed", false, "Use framed transport");
    options.addOption("c", "compact", false, "Use the compact protocol");
    options.addOption("h", "help", false, "Print help information");
    options.addOption(null, "infoport", true, "Port for web UI");

    val servers = new OptionGroup();
    servers.addOption(
      new Option("nonblocking", false, "Use the TNonblockingServer. This implies the framed transport."));
    servers.addOption(new Option("hsha", false, "Use the THsHaServer. This implies the framed transport."));
    servers.addOption(new Option("threadpool", false, "Use the TThreadPoolServer. This is the default."));
    options.addOptionGroup(servers);
    options;
  }

}