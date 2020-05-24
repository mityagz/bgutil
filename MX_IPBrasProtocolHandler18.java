package ru.hypernet.bgbilling.inet.dyn.device.radius;

import bitel.billing.server.radius.RadiusStandartAttributes;
import org.apache.log4j.Logger;
import ru.bitel.bgbilling.kernel.network.radius.RadiusAttribute;
import ru.bitel.bgbilling.kernel.network.radius.RadiusDictionary;
import ru.bitel.bgbilling.kernel.network.radius.RadiusPacket;
import ru.bitel.bgbilling.modules.inet.access.sa.ProtocolHandler;
import ru.bitel.bgbilling.modules.inet.access.sa.ProtocolHandlerAdapter;
import ru.bitel.bgbilling.modules.inet.radius.InetRadiusHelperProcessor;
import ru.bitel.bgbilling.modules.inet.radius.InetRadiusProcessor;
import ru.bitel.common.sql.ConnectionSet;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;


// username like  bras0:ge-0/0/4.1073741835:300-10 -> bras0:300-10

// For Fast Ethernet or Gigabit Ethernet interfaces that use S-VLANs: (fe | ge)-fpc/pic/port:svlan-id-vlan-id
// radius user-name = svid:cvid(create service activator, qinq vlan create), bgvlanid(remote) = svid, bginterface = cvid,
// dhcp.key = bgvlanid:bginterface


// #radius.key.pattern=$mac
// #dhcp.key.pattern=$mac
// $mac should be from Calling-Station-ID!!!

public class MX_IPBrasProtocolHandler17 extends ProtocolHandlerAdapter implements ProtocolHandler {
    private static int INET_MODULE_ID = 13;
    protected static final Logger logger = Logger.getLogger(MX_IPBrasProtocolHandler17.class);

    @Override
    public void preprocessAccessRequest(RadiusPacket request, RadiusPacket response, ConnectionSet connectionSet) throws Exception {
        super.preprocessAccessRequest(request, response, connectionSet);
        String userName = request.getStringAttribute(-1, RadiusDictionary.User_Name, null);
        String userNameRet = "";
        logger.info("HN Rad Access preprocess before: " + userName);

        RadiusAttribute<?> agentRemoteId = request.getAttribute(4874, 55);
        RadiusAttribute<?> macERX = request.getAttribute(4874, 56);
        if (agentRemoteId != null) {
            byte[] data = agentRemoteId.getDataAsByteArray();
            HashMap<Integer, Byte[]> optDHCP = getDHCPOptions(data);
            String opt82 = new String(btoB(getDHCP82(optDHCP.get(82), 1)));
            String [] opt82p = opt82.split(":");
            //!!! It is importance change !!!
            // This string is hard code for remote id "bras0", for develops bras redundance, by Mitya, 23.05.2020, str 49-51
            //String remoteId = opt82p[0] + "-" + opt82p[2].split("-")[0];
            String remoteId = "bras0" + "-" + opt82p[2].split("-")[0];
            
            logger.info("Rad attr 4874-55: remote:" + remoteId);

            //request.setOption(InetRadiusHelperProcessor.INTERFACE_ID, 5);
            //request.setOption(InetRadiusProcessor.INTERFACE_ID, 5);
            request.setOption(InetRadiusHelperProcessor.VLAN_ID, opt82p[2].split("-")[1]);
            request.setOption(InetRadiusProcessor.VLAN_ID, opt82p[2].split("-")[1]);
            request.setOption(InetRadiusProcessor.AGENT_REMOTE_ID, remoteId);
            request.setOption(InetRadiusHelperProcessor.AGENT_REMOTE_ID, remoteId);
        }

        if (macERX != null) {
             String mac = String.valueOf(macERX);
             String[] macp = mac.split("\\.");
             String macs = macp[0] + macp[1] + macp[2];
             request.setOption(InetRadiusProcessor.MAC_ADDRESS, macs);
             request.setOption(InetRadiusProcessor.MAC_ADDRESS_BYTES, macs.getBytes());
             request.setStringAttribute(RadiusStandartAttributes.Calling_Station_Id, macs);
        }

        /*
        if (userName.contains(":")) {
            logger.info("HN Rad Access preprocess after: " + userNameRet);
            int i0 = userName.indexOf(":");
            int i1 = userName.indexOf(":", i0 + 1);
            String mac = userName.substring(i0 + 1, i1);
            userNameRet = userName.substring(0, i0) + userName.substring(i1);
            request.setStringAttribute(-1, RadiusDictionary.User_Name, userNameRet);
            logger.info("MX_IPBrasProtocolHandler Username:" + userNameRet);
        }
        */
    }

    @Override
    public void preprocessAccountingRequest(RadiusPacket request, RadiusPacket response, ConnectionSet connectionSet) throws Exception {
        super.preprocessAccountingRequest(request, response, connectionSet);
        String userName = request.getStringAttribute(-1, RadiusDictionary.User_Name, null);
        String userNameRet = "";
        logger.info("HN Rad Access preprocess before: " + userName);

        RadiusAttribute<?> agentRemoteId = request.getAttribute(4874, 55);
        RadiusAttribute<?> macERX = request.getAttribute(4874, 56);
        if (agentRemoteId != null) {
            byte[] data = agentRemoteId.getDataAsByteArray();
            HashMap<Integer, Byte[]> optDHCP = getDHCPOptions(data);
            String opt82 = new String(btoB(getDHCP82(optDHCP.get(82), 1)));
            String [] opt82p = opt82.split(":");
            
            //String remoteId = opt82p[0] + "-" + opt82p[2].split("-")[0];
            //!!! It is importance change !!!
            // This string is hard code for remote id "bras0", for develops bras redundance, by Mitya, 23.05.2020, str 49-51
            //String remoteId = opt82p[0] + "-" + opt82p[2].split("-")[0];
            String remoteId = "bras0" + "-" + opt82p[2].split("-")[0];
            logger.info("Rad attr 4874-55: remote:" + remoteId);

            //request.setOption(InetRadiusHelperProcessor.INTERFACE_ID, 5);
            //request.setOption(InetRadiusProcessor.INTERFACE_ID, 5);
            request.setOption(InetRadiusHelperProcessor.VLAN_ID, opt82p[2].split("-")[1]);
            request.setOption(InetRadiusProcessor.VLAN_ID, opt82p[2].split("-")[1]);
            request.setOption(InetRadiusProcessor.AGENT_REMOTE_ID, remoteId);
            request.setOption(InetRadiusHelperProcessor.AGENT_REMOTE_ID, remoteId);
        }

        if (macERX != null) {
            String mac = String.valueOf(macERX);
             String[] macp = mac.split("\\.");
             String macs = macp[0] + macp[1] + macp[2];
             request.setOption(InetRadiusProcessor.MAC_ADDRESS, macs);
             request.setOption(InetRadiusProcessor.MAC_ADDRESS_BYTES, macs.getBytes());
             request.setStringAttribute(RadiusStandartAttributes.Calling_Station_Id, macs);
        }

        /*
        if (userName.contains(":")) {
            logger.info("HN Rad Access preprocess after: " + userNameRet);
            int i0 = userName.indexOf(":");
            int i1 = userName.indexOf(":", i0 + 1);
            String mac = userName.substring(i0 + 1, i1);
            userNameRet = userName.substring(0, i0) + userName.substring(i1);
            request.setStringAttribute(-1, RadiusDictionary.User_Name, userNameRet);
            logger.info("MX_IPBrasProtocolHandler Username:" + userNameRet);
        }
        */

        // remoteId && vlanId from username, only for accounting packet
        int i0 = 0, i1 = 0;
        i0 = userName.indexOf(":");
        i1 = userName.indexOf(":", i0 + 1);
        String vid = "", rid = "";
        vid = userName.substring(i1 + 1);
        rid = userName.substring(0, i0);
        request.setOption(InetRadiusHelperProcessor.VLAN_ID, vid.split("-")[1]);
        request.setOption(InetRadiusProcessor.VLAN_ID, vid.split("-")[1]);
        
        //!!! It is importance change !!!
        // This string is hard code for remote id "bras0", for develops bras redundance, by Mitya, 23.05.2020
        rid = "bras0";
            
        request.setOption(InetRadiusProcessor.AGENT_REMOTE_ID, rid + "-" +vid.split("-")[0]);
        request.setOption(InetRadiusHelperProcessor.AGENT_REMOTE_ID, rid + "-" + vid.split("-")[0]);

    }

    private String bb_to_str(ByteBuffer buff, Charset charset) {
        byte[] bytes;
        if (buff.hasArray()) {
            bytes = buff.array();
        } else {
            bytes = new byte[buff.remaining()];
            buff.get(bytes);
        }
        return new String(bytes);
    }


    private String bytesToHex(byte[] in) {
      final StringBuilder builder = new StringBuilder();
      for(byte b : in) {
         builder.append(Integer.toString(b, 16));
      }
      return builder.toString();
   }

    private HashMap<Integer, Byte []> getDHCPOptions(byte [] data) {
        // 1 35(dhcp mes type)01 01 3204 0AAE087F 370D 011C02790F060C28292A1A7703 52(82 opt)38 011A67652D302F302F342E313037333735313139373A3330302D3130 021A67652D302F302F342E3    13037333735313139373A3330302D3130
        // 2 ge-0/0/4.1073751197:300-10
        // 3 ge-0/0/4.1073751197:300-10
        HashMap<Integer, Byte[]> opt = new HashMap<Integer, Byte[]>();
        int i = 0, len = 0;
        Byte [] bb = new Byte[data.length];
        while(i < data.length) {
          bb[i] = data[i];
          i++;
        }

        i = 0;
        while(i < bb.length) {
         int bi = bb[i].intValue();
         len = bb[i + 1].intValue();
         opt.put(bi, Arrays.copyOfRange(bb, i + 2, i + 2 + len));
         i = i + 2 + len;
        }
        return opt;
    }

    private Byte[] getDHCP82(Byte [] b, int nsub) {
        int len;
        int opt;
        opt = b[0].intValue();
        len = b[1].intValue();
        logger.info("OPT82len " + len);
        return Arrays.copyOfRange(b, 2, len + 2);
    }

    private byte [] btoB (Byte[] bb) {
        byte [] ba = new byte[bb.length];
        int j = 0;
        for(Byte b: bb)
         ba[j++] = b.byteValue();
        return ba;
    }
}
