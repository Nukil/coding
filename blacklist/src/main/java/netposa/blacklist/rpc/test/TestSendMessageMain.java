package netposa.blacklist.rpc.test;


import netposa.blacklist.rpc.ChangeMetadataService;
import netposa.blacklist.rpc.Request;
import netposa.blacklist.rpc.Response;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

public class TestSendMessageMain {
  private static int timeout = 20000;
  private static boolean framed = true;
  private static boolean isCompact = true;

  public static void main(String[] args) {
    TTransport transport = new TSocket("192.168.61.15", 31050, timeout);
    if (framed) {
        transport = new TFramedTransport(transport);
    }
    TProtocol protocol = null;
    if (isCompact) {
        protocol = new TCompactProtocol(transport);
    }else {
        protocol = new TBinaryProtocol(transport);
    }
    
    ChangeMetadataService.Iface client = new ChangeMetadataService.Client(protocol);
    Request req = new Request();
    req.setControl_id("123");
    try {
      transport.open();
      Response response = client.sendRequest(req);
      System.out.println(response.flag + "  " +response.err_msg);
      transport.close();
    } catch (TException e) {
      e.printStackTrace();
    }
    
  }

}
