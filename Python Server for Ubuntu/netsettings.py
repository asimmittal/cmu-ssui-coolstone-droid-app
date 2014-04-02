"""
	testServer.py
	--------------
	Test program for Socket Server  
"""
import os,commands;
from udpbroadcaster import *;
from socketserver import *;

def getBroadcastAddress():

	"""
	platform independent ip and broadcast address retriever. returns (bcast,ip) addresses 
	in a tuple
	"""
	bcast = '127.0.0.1'
	ip = '127.0.0.1'
	
	if(os.name.lower() == 'posix'):
		bcast,ip = getBroadcastAddressPosix()
	elif(os.name.lower() == 'nt'):
		bcast,ip = getBroadcastAddressNt()
	
	return bcast,ip
	
	
def getBroadcastAddressNt():
	"""
	this function will get the IP address of the host and use that to create the 
	broadcast address. Works well on Windows
	"""
	import socket;
	netInfo = socket.gethostbyname_ex(socket.gethostname())
	ipAddr = (netInfo[2])[0]
	segments = ipAddr.split(".")
	segments[-1] = "255"
	broadcastAddr = (".").join(segments)
	return broadcastAddr,ipAddr

def getBroadcastAddressPosix():
	"""
	This function will grep the broadcast ip address from the 'ifconfig' output
	and return it as a string. This is for linux or unix based machines
	"""
	import socket;
	broadcastAddr = '127.0.0.1'
	ipAddr = '127.0.0.1'

	tagBcast = "Bcast:"
	tagIpadd = "addr:"
	cmd = "ifconfig | grep Bcast:"
	
	#this string looks like "inet addr:192.168.1.1 Bcast:192.168.1.255"
	#this contains enough data to pull out the ip and broadcast address
	strOutput = commands.getoutput(cmd);
	
	#just double check that both fields exist in the output
	if((tagBcast in strOutput) and (tagIpadd in strOutput)):
		
		#first lets grab the broadcast address
		indexStart = strOutput.find(tagBcast);
		indexEnd = strOutput.find(" ",indexStart)
		broadcastAddr = strOutput[indexStart:indexEnd].replace(tagBcast,"");

		#now the bcast addr looks like 192.168.1.255. This might be great for smaller personal wifi networks
		#but if you're on a larger network (for example a univ. network), subnets are pretty large
		#so just to be on the safe side, let's mask the last two segments of the IP addreess 
		#byteGroup = broadcastAddr.split(".")                   #so i'll split the address into segments ['192', '168', '1', '255']
		#byteGroup[-1] = '255';byteGroup[-2]='255'                                      #alter this list of segments ['192', '168', '255', '255']
		#broadcastAddr = (".").join(byteGroup)                  #join them using a "." as a separator -> "192.168.255.255"

		#now that the broadcast address is good, let's get the individual ip address of the host
		indexStart = strOutput.find(tagIpadd);
		indexEnd = strOutput.find(" ",indexStart);
		ipAddr = strOutput[indexStart:indexEnd].replace(tagIpadd,"");

	#return the broadcast and ip address of the host
	return broadcastAddr,ipAddr


def getDeviceName():
	"""
	This routine will take in the IP Address, the port number and create the message
	to broadcast on UDP. The message will look like this:

	192.168.56.1;52225;asim-notebook

	each of the important fields is separated by semicolons.
	"""
	import socket
	deviceName = socket.getfqdn()
	return deviceName


