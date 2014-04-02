"""
	UDPBroadcaster.py
	-----------------
"""

import sys,time,threading,random
from socket import *

class UDPBroadcaster:

	def __init__(self,msg,port,ipaddr,sleeptime,addRandomChars):
		self.port = port;
		self.message = msg;
		self.ipAddr = ipaddr;
		self.isRunning = False;
		self.naptime = sleeptime;
		self.toAddRandom = addRandomChars;
		
		self.sock = socket(AF_INET,SOCK_DGRAM);
		self.sock.setsockopt(SOL_SOCKET,SO_BROADCAST,1);
		self.sock.bind(('',self.port));
		
	
	def startBroadcasting(self):
		self.thrdBroadcaster = threading.Thread(target=self.run);
		self.isRunning = True;
		self.thrdBroadcaster.start()
		
	def stopBroadcasting(self):
		self.isRunning = False;
		
	def run(self):
		
		time.sleep(2)
		print 'broadcasting to:',(self.ipAddr,self.port)
		
		counter = 0;
		while self.isRunning:
			
			counter += 1;
			
			try:
				toSend = self.message;
				if(self.toAddRandom is True):
					toSend = self.message + ";" + str(random.randint(0,100)) 
					
				self.sock.sendto(toSend,(self.ipAddr,self.port))
				time.sleep(self.naptime)
			except:
				self.isRunning = False;
				self.sock.close()
				print 'Exception in UDPBroadcaster...'

		print 'UDP Broadcaster is dying...'
