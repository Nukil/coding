package netposa.fakecar.feature.rpc.util;

import java.lang.Thread.UncaughtExceptionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 线程失败时的异常处理流程
 * @author hongs.yang
 *
 */
public class ThreadExceptionHandler implements UncaughtExceptionHandler {
	private static final Logger LOG = LoggerFactory.getLogger(ThreadExceptionHandler.class);

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		LOG.warn("this thread " + t.getName() + ", execute error => " + e.getMessage(),e);
	}

}
