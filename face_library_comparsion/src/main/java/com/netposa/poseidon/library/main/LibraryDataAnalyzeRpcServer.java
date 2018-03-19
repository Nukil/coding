package com.netposa.poseidon.library.main;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import com.netposa.poseidon.library.rpc.LibraryDataAnalyzeRpcService;
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


public class LibraryDataAnalyzeRpcServer implements Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(LibraryDataAnalyzeRpcServer.class);
	public final int port;

	public LibraryDataAnalyzeRpcServer(int port) {
		this.port = port;
	}
	/**
	 * 协议/客户端和服务端必须保持一致
	 * @param isCompact
	 * @return
	 */
	private TProtocolFactory getTProtocolFactory(boolean isCompact) {
		if (isCompact) {
			LOG.debug("Using compact protocol");
			return new TCompactProtocol.Factory();
		} else {
			LOG.debug("Using binary protocol");
			return new TBinaryProtocol.Factory();
		}
	}
	
	private TTransportFactory getTTransportFactory(boolean framed, int frameSize) {
		if (framed) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Using framed transport");
			}
			return new TFramedTransport.Factory(frameSize);
		} else {
			return new TTransportFactory();
		}
	}
	
	private InetSocketAddress bindToPort(String bindValue, int listenPort) throws UnknownHostException {
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
	private TServer getTThreadPoolServer(TProtocolFactory protocolFactory, TProcessor processor, TTransportFactory transportFactory, InetSocketAddress inetSocketAddress) throws TTransportException {
		TServerTransport serverTransport = new TServerSocket(inetSocketAddress);
		LOG.info("Starting FaceFeatureDataAyalyzeSearchRpcServer ThreadPool Thrift server on " + inetSocketAddress.toString());
		TThreadPoolServer.Args serverArgs = new TThreadPoolServer.Args(serverTransport);
		serverArgs.processor(processor);
		serverArgs.transportFactory(transportFactory);
		serverArgs.protocolFactory(protocolFactory);
		return new TThreadPoolServer(serverArgs);
	}

	@Override
	public void run() {
		final LibraryDataAnalyzeRpcServiceHandler handler = new LibraryDataAnalyzeRpcServiceHandler();
		final LibraryDataAnalyzeRpcService.Processor<LibraryDataAnalyzeRpcServiceHandler> p = new LibraryDataAnalyzeRpcService.Processor<LibraryDataAnalyzeRpcServiceHandler>(handler);
		TProcessor processor = p;
		TServer server = null;
		String bindAddress = "0.0.0.0";
		try {
			TProtocolFactory protocolFactory = getTProtocolFactory(true);
			TTransportFactory transportFactory = getTTransportFactory(true, 2 * 1024 * 1024);
			InetSocketAddress inetSocketAddress = bindToPort(bindAddress, port);
			server = getTThreadPoolServer(protocolFactory, processor, transportFactory, inetSocketAddress);
			server.serve();
		} catch (Exception e) {
			LOG.error("Start FaceFeatureDataAyalyzeSearchRpcServer Failure.", e);
		}
	}

}
