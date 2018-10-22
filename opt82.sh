#!/bin/bash

#53,54,51,1,3,6,82,0	47:45:50:4f:4e:2d:4c:54:45:2d:38:30:32:2d:75:64:61:72:37:61:20:78:70:6f:6e:20:34:20:31:30:33:33:20:31:20:32:33:20:30		e8:94:f6:03:c8:1b	192.168.10.32	185.190.1.133

# 53,54,51,1,3,6,82,0	00:0c:01:12	a8:f9:4b:38:59:40	c8:60:00:a8:20:b3	192.168.10.120	192.168.29.34

declare -A ipopt


SAVEIFS=${IFS}
IFS='\n'
while read rawstr  
do
IFS=${SAVEIFS}
	rawar=($(echo  "${rawstr}"))
IFS='\n'
agent_circuit_id=$(echo -n ${rawar[1]})
if [[ ${#agent_circuit_id} -ge 20 ]];then
	agent_remote_id=$(echo -n ${rawar[2]})
	hw_mac_addr=$(echo -n ${rawar[3]})
	ip_relay=$(echo -n ${rawar[4]})
	ip_your=$(echo -n ${rawar[5]})
	circuitid=$(echo ${agent_circuit_id} | while read -d":" c; do echo -n  0x$c | xxd -r ; done)
	#echo LTE
	IFS=${SAVEIFS}
	declare cir=($(echo ${circuitid#GEPON-LTE-.* xpon 0}))
	interfaceId=$(echo ${cir[3]})
	vlanid=$(echo ${cir[5]})
	#echo $circuitid $agent_remote_id $hw_mac_addr $ip_relay $ip_your $interfaceId $vlanid
	###!echo $agent_remote_id $hw_mac_addr $ip_relay $ip_your $interfaceId $vlanid
		#ipopt["${ip_relay}"]="$agent_remote_id $hw_mac_addr $ip_relay $ip_your $interfaceId $vlanid"
		ipopt["${ip_relay}"]="$agent_remote_id,$hw_mac_addr,$ip_relay,$ip_your$vlanid,$interfaceId"
	IFS='\n'

else
	agent_remote_id=$(echo -n ${rawar[2]})
	hw_mac_addr=$(echo -n ${rawar[3]})
	ip_relay=$(echo -n ${rawar[4]})
	ip_your=$(echo -n ${rawar[5]})
	if [[ ${#agent_circuit_id} -eq 11 ]];then
		p=""
		v=""
		p=$(echo ${agent_circuit_id} |  awk -F':' '{print $4}' | tr "a-h" "A-H")
		v=$(echo ${agent_circuit_id} |  awk -F':' '{print $1$2}' | tr "a-h" "A-H")
		interfaceId=$(echo "ibase=16; ${p}" | bc)
		vlanId=$(echo "ibase=16; ${v}" | bc)
		#interfaceId=$(echo ${agent_remote_id} | awk -F':' '{print $4}')
		#echo $agent_circuit_id $agent_remote_id $hw_mac_addr $ip_relay $ip_your $interfaceId $vlanId
		###!echo $hw_mac_addr $ip_relay $ip_your $interfaceId $vlanId
		#ipopt["${ip_your}"]="$hw_mac_addr $ip_relay $ip_your $interfaceId $vlanid"
		ipopt["${ip_your}"]="$hw_mac_addr,$ip_relay,$ip_your,$vlanid,$interfaceId"
	else 	#echo "SW without opt82"
		p=""
		v=""
		p=$(echo ${agent_circuit_id} |  awk -F':' '{print $4}' | tr "a-h" "A-H")
		v=$(echo ${agent_circuit_id} |  awk -F':' '{print $1$2}' | tr "a-h" "A-H")
		#echo $hw_mac_addr $ip_relay $ip_your
		#echo ${rawstr}
	fi
fi
done<<<$(tshark  -Y "bootp.option.type == 82" -Y "bootp.ip.your != 0.0.0.0" -T fields -r dhcpdWLOPT82.pcap   -e bootp.option.type -e bootp.option.agent_information_option.agent_circuit_id -e bootp.option.agent_information_option.agent_remote_id -e bootp.hw.mac_addr -e bootp.ip.relay -e bootp.ip.your)

#for addr in ${!ipopt[@]}
#do 
# echo "$addr ::" "${ipopt[$addr]}"; 
#done



cat bil.txt |     grep -v "mysql\|--------\|fio" | grep -v "mysql\|--------\|fio\|rows" |  while IFS="|" read fio local_ip address tarif_speed_in sector iface vlanid routers subnet mask broadcast
 

do
fio=$(echo -n $fio $local_ip'|'$address  | awk -F'|' '{print $1}')
ip=$(echo -n $fio $local_ip'|'$address  | awk -F'|' '{print $2}' | sed 's/ //g')
 mp=$(grep  -B4   "$ip\;"  dhcpd.conf | grep "hardware ethernet\|match if")
 if [ -n "${mp}" ]
 then
	 if echo "${mp}" | grep "hardware ethernet" 2>&1 > /dev/null
	 then
		 macl=$(echo "${mp}" | awk '{print $3}')
		 mac=$(echo ${macl::-1} | tr "A-F" "a-f")
		 echo "${fio}|${ip}|${mac}|${ipopt[$ip]}"
	 elif echo "${mp}" | grep "match if" 2>&1 > /dev/null
	 then
		 port=$(sed -e 's/.*= \("[0-9]\+"\).*= \("[0-9]\+"\).*= \(.*\))).*/\1,\2,\3/' -e 's/\"//g'  <(echo ${mp}));
		 echo "${fio}|${ip}|${port}|${ipopt[$ip]}"
	 fi
 fi
 ip=""
 mp=""
 port=""
 fio=""
 local_ip=""
 address=""
done
