package icblive.chatserver.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class LoggerUtils {
	private static Logger logger = LogManager.getLogger(LoggerUtils.class.getName());
	public static void printExceprionLogger(Exception e) {
		if (e == null) {
			return;
		}
		logger.error("Exception:" + e.toString());
		StackTraceElement[] trace = e.getStackTrace();
		for (StackTraceElement traceElement : trace)
			logger.error("Exception:\tat " + traceElement);
	}
	public static void printExceprionLogger(Throwable e) {
		if (e == null) {
			return;
		}
		logger.error("Exception:" + e.toString());
		StackTraceElement[] trace = e.getStackTrace();
		for (StackTraceElement traceElement : trace)
			logger.error("Exception:\tat " + traceElement);
	}
}

