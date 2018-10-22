package ru.nh.bgbilling.script.global;

/**
 * Created by mitya on 8/25/17.
 */

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import java.io.*;
import java.net.*;
import java.util.regex.Pattern;


import bitel.billing.server.contract.bean.*;
import com.hazelcast.mapreduce.impl.HashMapAdapter;
import org.apache.log4j.Logger;

import ru.bitel.bgbilling.common.BGException;
import ru.bitel.bgbilling.kernel.container.managed.ServerContext;
import ru.bitel.bgbilling.kernel.script.server.dev.GlobalScriptBase;
import ru.bitel.bgbilling.modules.inet.api.common.bean.InetDevice;
import ru.bitel.bgbilling.modules.inet.api.common.bean.InetServ;
import ru.bitel.bgbilling.modules.inet.api.common.bean.InetServOption;
import ru.bitel.bgbilling.modules.inet.api.common.service.InetDeviceService;
import ru.bitel.bgbilling.modules.inet.api.common.service.InetServService;
import ru.bitel.bgbilling.modules.inet.api.server.bean.InetDeviceDao;
import ru.bitel.bgbilling.server.util.Setup;
import ru.bitel.common.Utils;
import ru.bitel.common.sql.ConnectionSet;
import bitel.billing.common.TimeUtils;
import ru.bitel.bgbilling.modules.inet.api.server.InetDeviceDao0;

public class VLServ extends GlobalScriptBase {
	private static int INET_MODULE_ID = 13;
    private static int INET_SERV_TYPE_1 = 25;
    private static int INET_SERV_TYPE_2 = 24;
    private static int INET_DEVICE_ACCESS_FOLDER_ID = 1226;
    private static int INET_VL_GROUP_ID = 22;

	private InetServService wsServ = null;
	private InetDeviceService wsDevice = null;

    aUserEntry au;
    HashMap<String, Integer> cidmap;
    HashMap<Integer, fUserEntry> db;



	Logger logger = Logger.getLogger( gTest.class );

	@Override
	public void execute( Setup setup, ConnectionSet connectionSet ) throws Exception {
		ServerContext context = ServerContext.get();
		wsServ = context.getService( InetServService.class, INET_MODULE_ID );
		wsDevice = context.getService( InetDeviceService.class, INET_MODULE_ID );
		cidmap = getContractId(connectionSet);
        au = new aUserEntry();
 		getUserData();
        createDb();
        createServices ( connectionSet );
	}


    private void createDb() {
        db = new HashMap<Integer, fUserEntry>();
        for(String s  : au.au.keySet()) {
            if(cidmap.get(au.au.get(s).getFio()) != null) {
                db.put(cidmap.get(au.au.get(s).getFio()), au.au.get(s));
            } else {
                print("Not found in the bg: " + au.au.get(s).getFio());
            }
        }
    }


    private class fUserEntry {
        int type;
        String fio;
        String ip;
        String dhcpMac;
        int dhcpVlan;
        int dhcpPort;
        String dhcpIpDevice;

        String pcapMac;
        int pcapVlan;
        int pcapPort;
        String pcapIpDevice;

        fUserEntry(int type, String fio, String ip, int dhcpVlan, int dhcpPort, String dhcpIpDevice) {
            this.type = type;
            this.fio = fio;
            this.ip = ip;
            this.dhcpVlan = dhcpVlan;
            this.dhcpPort = dhcpPort;
            this.dhcpIpDevice = dhcpIpDevice;
        }

        fUserEntry(int type, String fio, String ip, String dhcpMac) {
            this.type = type;
            this.fio = fio;
            this.ip = ip;
            this.dhcpMac = dhcpMac;
        }

        public String getFio() {
            return fio;
        }

        public String toString() {
            if(type == 2)
                return type + " " + fio + " " + ip + " " + dhcpVlan + " " + dhcpPort + " " + dhcpIpDevice;
            return type + " " + fio + " " + ip + " " + dhcpMac;
        }
    }

    private class aUserEntry {
            HashMap<String, fUserEntry> au;
            aUserEntry() {
                au = new HashMapAdapter<String, fUserEntry>();
            }
        public void add(fUserEntry fu) {
            au.put(fu.fio, fu);
        }

    }

	private void getUserData() {
	    FileReader fileReader = null;
		BufferedReader br = null;
		String line;
        String mac = null;
        String portvlan;
        int vlan = 0;
        int port = 0;
        String out = "";
        String relay = "";
        int nstr = 0;
        fUserEntry fu;
        au = new aUserEntry();
		try {
			fileReader = new FileReader(new File("/tmp/out.all"));
			br = new BufferedReader(fileReader);
			while ((line = br.readLine()) != null) {
                mac = "";
                portvlan = "";
                vlan = 0;
                port = 0;
                relay = "";
                out = "";
                fu = null;
                String [] users = line.split("\\|");
                if(users.length > 2) {
                    String fio = users[0].trim();
                    String ip = users[1].trim();
                    out = fio + " " + ip;
                    if(Pattern.matches(".*:.*", users[2])) {
                        mac = users[2];
                        fu = new fUserEntry(1, fio, ip, mac);
                    } else {
                        portvlan = users[2];
                        vlan = Integer.parseInt(portvlan.split("\\,")[0]);
                        port = Integer.parseInt(portvlan.split("\\,")[1]);
                        relay = portvlan.split("\\,")[2];
                        fu = new fUserEntry(2, fio, ip, vlan, port, relay);
                    }
                    nstr++;
                    if(fu != null) {
                        au.add(fu);
                    }
                }
 			}
		} catch (IOException e) { e.printStackTrace(); }
	}

	private HashMap<String, Integer> getContractId(ConnectionSet connectionSet) {
		Connection con = connectionSet.getConnection();
		ContractUtils cu = new ContractUtils(con);
        ContractManager cm = new ContractManager(con);
        Contract ct;
        HashMap<String, Integer> cidmap = new HashMap<String, Integer>();

		String cids = cu.getCids(1 << INET_VL_GROUP_ID);
		String [] cid = cids.split(",");

		for(String c : cid) {
            ct = cm.getContractById(Integer.parseInt(c.trim()));
            if(ct != null) {
                String [] comment = ct.getComment().split(" ");
                String com = "";
                for(int i = 0; i < comment.length - 1; i++) {
                    com += comment[i] + " ";
                }
                cidmap.put(com.trim(), Integer.parseInt(c.trim()));
            }
		}
        return  cidmap;
	}

	private void createServices( ConnectionSet connectionSet )  throws SQLException, BGException {
        HashMap<String, Integer> ipDevId = new HashMap<String, Integer>();

	    Connection con = connectionSet.getConnection();


		connectionSet.commit();
		logger.info( "creating services..." );


		ContractModuleManager cmm = new ContractModuleManager( con );

		int cid = 0;
        int cnt = 0;

        InetDeviceDao deviceDao = new InetDeviceDao(con, INET_MODULE_ID);
        List<InetDevice> devList = deviceDao.listSource();

        for(InetDevice dev : devList) {
            ipDevId.put(dev.getHost(), dev.getId());
        }

         for(Integer i : db.keySet()) {
             // delete services typeId are 24, 25
             List<InetServ> list = wsServ.inetServList(i);
             for (InetServ oldserv : list) {
                 print("Oldserv: " + oldserv);
                 if (oldserv != null && (oldserv.getTypeId() == INET_SERV_TYPE_1 || oldserv.getTypeId() == INET_SERV_TYPE_2)) {
                     oldserv.setDateTo(TimeUtils.getPrevDay(new Date()));
                     wsServ.inetServUpdate(oldserv, new ArrayList<InetServOption>(), false, false, 0);
                     try {
                         wsServ.inetServDelete(oldserv.getId());
                     } catch (Exception e) {
                         print(oldserv.getContractId() + "Error while deleting extra  service date for contract " + oldserv.getContractId() + ":");
                         connectionSet.commit();
                         continue;
                     }
                 }
             }

             if(db.get(i).type == 2 && ipDevId.get(db.get(i).dhcpIpDevice) != null) {
                 //if(i == 5116) {
                        print("cid:" + i + " " + db.get(i) + " " + ipDevId.get(db.get(i).dhcpIpDevice));
                        InetServ inetServ = new InetServ();
                        inetServ.setContractId(i);
                        inetServ.setLogin("");
                        inetServ.setTypeId(INET_SERV_TYPE_2);
                        inetServ.setDeviceId(ipDevId.get(db.get(i).dhcpIpDevice));
                        inetServ.setInterfaceId(db.get(i).dhcpPort);
       	                inetServ.setDateFrom(new Date());
                        inetServ.setStatus(0);
                        inetServ.setSessionCountLimit(1);

                        byte[] ipBytes = null;

                        try {
        	                String ip = db.get(i).ip;
        	                InetAddress ipa = InetAddress.getByName(ip);
			                ipBytes = ipa.getAddress();
        	                inetServ.setAddressFrom(ipBytes);
                        }catch(Exception e) {
        	                System.out.println(e);
                        }
	    	            int serviceId = wsServ.inetServUpdate( inetServ, new ArrayList<InetServOption>(), false, false, 0 );
                        cnt++;
                 //}

             } else if(db.get(i).type == 1){
                 //if(i == 5213){
                        print("cid:" + i + " " + db.get(i));
                        InetServ inetServ = new InetServ();
                        inetServ.setContractId(i);
                        inetServ.setLogin("");
                        inetServ.setTypeId(INET_SERV_TYPE_1);
                        inetServ.setDeviceId(INET_DEVICE_ACCESS_FOLDER_ID);
                        inetServ.setDateFrom(new Date());
                        inetServ.setStatus(0);
                        inetServ.setSessionCountLimit(1);

                        byte[] ipBytes = null;
                        try {
        	                String ip = db.get(i).ip;
        	                InetAddress ipa = InetAddress.getByName(ip);
			                ipBytes = ipa.getAddress();
        	                inetServ.setAddressFrom(ipBytes);
                        }catch(Exception e) {
        	                System.out.println(e);
                        }

                        try {
        	                String mac = db.get(i).dhcpMac;
                            inetServ.setMacAddressList(getMacList(mac));
                        } catch(Exception e) {
        	                System.out.println(e);
                        }
                        int serviceId = wsServ.inetServUpdate( inetServ, new ArrayList<InetServOption>(), false, false, 0 );
                        cnt++;
                 //}
             }
        }
        /*
           2: cid + serviceType(24) + deviceId(from ip relay) + Interface + ip
           1: cid + serviceType(25) + deviceId(1226) + mac + ip
        */
        print("CNT: " + cnt);
	}


    public List<byte []> getMacList(String macAddress) {
        String[] macAddressParts = macAddress.split(":");
        byte[] macAddressBytes = new byte[6];
        for (int i = 0; i < 6; i++) {
            Integer hex = Integer.parseInt(macAddressParts[i], 16);
            macAddressBytes[i] = hex.byteValue();
        }
        List<byte[]> blist = new LinkedList<byte[]>();
        blist.add(macAddressBytes);
        return blist;
    }
}

