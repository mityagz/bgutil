package ru.nh.bgbilling.inet.dyn.device.dhcp;

/**
 * Created by mitya on 8/24/18.
 */

// SA imports

import net.juniper.netconf.CommitException;
import net.juniper.netconf.Device;
import net.juniper.netconf.LoadException;
import org.apache.log4j.Logger;
import org.snmp4j.smi.OID;
import ru.bitel.bgbilling.kernel.container.managed.ServerContext;
import ru.bitel.bgbilling.kernel.network.snmp.SnmpClient;
import ru.bitel.bgbilling.modules.inet.access.sa.ServiceActivator;
import ru.bitel.bgbilling.modules.inet.access.sa.ServiceActivatorAdapter;
import ru.bitel.bgbilling.modules.inet.access.sa.ServiceActivatorEvent;
import ru.bitel.bgbilling.modules.inet.api.common.bean.InetDevice;
import ru.bitel.bgbilling.modules.inet.api.common.bean.InetDeviceType;
import ru.bitel.bgbilling.modules.inet.api.common.service.InetDeviceService;
import ru.bitel.bgbilling.modules.inet.api.common.service.InetServService;
import ru.bitel.bgbilling.server.util.Setup;
import ru.bitel.common.ParameterMap;
import ru.bitel.oss.systems.inventory.resource.server.bean.VlanCategoryDao;
import ru.bitel.oss.systems.inventory.resource.server.bean.VlanResourceDao;

import java.sql.Connection;

/*
 * NH This class manages Juniper MX interfaces,
 * for qinq subscriber management.
*/

/*
 * sa.snmp.disconnect.mode=
 * sa.snmp.disconnect.oid=
 * sa.snmp.disconnect.value=
 * sa.snmp.connection.key.field=
 * sa.snmp.connection.key.offset=
 * sa.snmp.connection.key.length=
 * sa.snmp.connection.key.mode=
 */

/* Example qinq interface with push on NNI
   show interfaces ge-0/0/4
    flexible-vlan-tagging;
    native-vlan-id 444;
    encapsulation flexible-ethernet-services;
    unit 444 {
     encapsulation vlan-bridge;
     vlan-id 444;
   }

   example with push on UNI
   unit 5555 {
    description 99d643132a99:5;
    encapsulation vlan-bridge;
    vlan-id 400;
    input-vlan-map {
        push;
        vlan-id 4094;
    }
    output-vlan-map pop;
   }
 */


public class nullswitch_ServiceActivator17 extends ServiceActivatorAdapter implements ServiceActivator {
	protected static final Logger logger = Logger.getLogger( nullswitch_ServiceActivator17.class );
	String snmpHost; 
    private int snmpVersion;
    private int snmpPort;
    private String snmpCommunity;
    private boolean checkUptime;
    protected SnmpClient snmpClient;
    protected boolean enabled;
    private OID uptimeOid;


    private static int INET_MODULE_ID = 13;
    private static int INET_SERV_TYPE = 27;
    private InetServService wsServ = null;
	private InetDeviceService wsDevice = null;
    private int brasDeviceId = 1271;
    private InetDevice accessDevice;
    private int serviceId = 0;
    private String login;
    private String password;
    private String accessDeviceIP;
    private Device ndev;
    private VlanCategoryDao vcd;
    private VlanResourceDao vrd;
    private int vlanCategoryId;
    private Connection con;
    private ServerContext context;

    

	public Object init(Setup setup, int moduleId, InetDevice device, InetDeviceType deviceType, ParameterMap deviceConfig) throws Exception {
		super.init( setup, moduleId, device, deviceType, deviceConfig );
        accessDevice = device;
        login = accessDevice.getUsername();
        password = accessDevice.getPassword();
        accessDeviceIP = accessDevice.getHost();
        vlanCategoryId = Integer.parseInt(deviceConfig.get("vlan.resource.category"));
        return null;
	}

	@Override
	public Object destroy() throws Exception {
		return super.destroy();
	}
	
	
	public Object connect() throws Exception {
        ndev = new Device(accessDeviceIP, login, password, null);
        ndev.connect();

        context = ServerContext.get();
        con = context.getConnection();
        vcd = new VlanCategoryDao(con, INET_MODULE_ID);
        vrd = new VlanResourceDao(con, INET_MODULE_ID);
        return true;
	}

	public Object disconnect() throws Exception {
        ndev.close();
        con.close();
        context.destroy();
		return true;
	}

	public Object serviceCreate( ServiceActivatorEvent e ) throws Exception {
        int cid = e.getNewInetServ().getContractId();
        int interfaceId = e.getNewInetServ().getInterfaceId();
        int accessDeviceId = e.getNewInetServ().getDeviceId();
        int innerVid = e.getNewInetServ().getVlan();

        //Lock the configuration first
            boolean isLocked = ndev.lockConfig();
            if (!isLocked) {
                ndev.unlockConfig();
                //ndev.close();
                return false;
            }

        String outerVid = String.valueOf(vcd.get(vlanCategoryId)).split("-")[1];
        String confIntf = "set interfaces ge-0/0/" + interfaceId + " unit " + innerVid + " encapsulation vlan-bridge vlan-id " + innerVid;
        String confBridge = "set bridge-domains VLAN_" + outerVid + " interface ge-0/0/" + interfaceId +"." + innerVid;

        if(nconf(ndev, confIntf, confBridge)) {
            return true;
        } else return false;
	}

    public Object serviceCancel( ServiceActivatorEvent e ) throws Exception {
        int cid = e.getOldInetServ().getContractId();
        int interfaceId = e.getOldInetServ().getInterfaceId();
        int accessDeviceId = e.getOldInetServ().getDeviceId();
        int innerVid = e.getOldInetServ().getVlan();

        //Lock the configuration first
            boolean isLocked = ndev.lockConfig();
            if (!isLocked) {
                ndev.unlockConfig();
                //ndev.close();
                return false;
            }

         // vcd.get(8) home_inner_vid_bras0-4093
        String outerVid = String.valueOf(vcd.get(vlanCategoryId)).split("-")[1];
        String confIntf = "delete interfaces ge-0/0/" + interfaceId + " unit " + innerVid;
        String confBridge = "delete bridge-domains VLAN_" + outerVid + " interface ge-0/0/" + interfaceId +"." + innerVid;

        if(nconf(ndev, confIntf, confBridge)) {
            return true;
        } else return false;
    }

    private boolean nconf(Device ndev, String confIntf, String confBridge) throws Exception {
        try {
            ndev.loadSetConfiguration(confIntf);
            ndev.loadSetConfiguration(confBridge);
            ndev.commit();
        } catch(LoadException le) {
            logger.info(le.getMessage());
        } catch(CommitException ce) {
            logger.info(ce.getMessage());
        } finally {
            ndev.unlockConfig();
            //ndev.close();
        }
        //ndev.close();
        return true;
    }

	public Object serviceModify(ServiceActivatorEvent e) throws Exception { return true; }
	public Object connectionModify(ServiceActivatorEvent e) throws Exception { return true; }
	public Object connectionClose(ServiceActivatorEvent e) throws Exception { return true; }
	public Object onAccountingStart(ServiceActivatorEvent e) throws Exception { return true; }
	public Object onAccountingStop(ServiceActivatorEvent e) throws Exception { return true; }
}
