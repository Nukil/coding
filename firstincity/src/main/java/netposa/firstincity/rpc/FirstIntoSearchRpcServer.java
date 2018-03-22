package netposa.firstincity.rpc;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import netposa.firstincity.rpc.FisrtIntoSearchRpcService.Processor;

import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.thrift.transport.TTransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FirstIntoSearchRpcServer extends Thread{
	
	private static final Logger LOG = LoggerFactory.getLogger(FirstIntoSearchRpcServer.class);

	public int port;
	private Properties props;

	public FirstIntoSearchRpcServer(int port, Properties props) {
		this.port = port;
		this.props = props;
	}

	public void run() {
		
		final FirstIntoSearchRpcServiceHandler searchHandler = new FirstIntoSearchRpcServiceHandler(props);
		final Processor<FirstIntoSearchRpcServiceHandler> p = new Processor<FirstIntoSearchRpcServiceHandler>(searchHandler);
		TProcessor processor = p;
		
		try {
			
			TServer server = null;
			
		    String bindAddress = "0.0.0.0";
		    
		    TProtocolFactory protocolFactory = getTProtocolFactory(false);
		    
		    TTransportFactory transportFactory = getTTransportFactory(false, 2*1024*1024);
		    
		    InetSocketAddress inetSocketAddress = bindToPort(bindAddress, port);
		    
		    server = getTThreadPoolServer(protocolFactory, processor, transportFactory, inetSocketAddress);
			
			server.serve();
			LOG.info("Starting the simple server with port : " + port);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private static TProtocolFactory getTProtocolFactory(boolean isCompact) {
	    if (isCompact) {
	      LOG.debug("Using compact protocol");
	      return new TCompactProtocol.Factory();
	    } else {
	      LOG.debug("Using binary protocol");
	      return new TBinaryProtocol.Factory();
	    }
	}
	
	private static TTransportFactory getTTransportFactory(boolean framed, int frameSize) {
		if (framed) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Using framed transport");
			}
		    return new TFramedTransport.Factory(frameSize);
		}else {
			return new TTransportFactory();
		}
	}
	
	private static TServer getTThreadPoolServer(TProtocolFactory protocolFactory, TProcessor processor,
		      TTransportFactory transportFactory, InetSocketAddress inetSocketAddress) throws TTransportException {
      TServerTransport serverTransport = new TServerSocket(inetSocketAddress);
		LOG.info("starting firstinto ThreadPool Thrift server on " + inetSocketAddress.toString());
		TThreadPoolServer.Args serverArgs = new TThreadPoolServer.Args(serverTransport);
		serverArgs.processor(processor);
		serverArgs.transportFactory(transportFactory);
		serverArgs.protocolFactory(protocolFactory);
		return new TThreadPoolServer(serverArgs);
	}
	
	private static InetSocketAddress bindToPort(String bindValue, int listenPort)
		throws UnknownHostException {
	    try {
	      if (bindValue == null) {
	        return new InetSocketAddress(listenPort);
	      } else {
	        return new InetSocketAddress(InetAddress.getByName(bindValue), listenPort);
	      }
	    } catch (UnknownHostException e) {
	      throw new RuntimeException("Could not bind to provided ip address", e);
	    }
	}

}