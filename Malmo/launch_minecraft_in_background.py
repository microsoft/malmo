import os
import socket
import subprocess
import sys
import time

sys.stdout = os.fdopen(sys.stdout.fileno(), 'w', 0)  # flush print output immediately

def PortHasListener( port ):
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    result = sock.connect_ex( ('127.0.0.1',port) )
    sock.close()
    return result == 0

if PortHasListener(10000):
    print 'Something is listening on port 10000 - will assume Minecraft is running.'
    exit(0)
    
print 'Nothing is listening on port 10000 - will attempt to launch Minecraft from a new terminal.'

path = os.path.join( os.getcwd(), "..", "Minecraft" )
if os.name == 'nt':
    subprocess.call( "start cmd /C \"cd " + path + " && launchClient -port 10000\"", shell=True )
else:
    subprocess.call( "xterm -e \"cd " + path + " && ./launchClient.sh -port 10000\"", shell=True )

print 'Giving Minecraft some time to launch: ',
for i in xrange( 100 ):
    print '.',
    time.sleep( 3 )
    if PortHasListener(10000):
        print 'ok'
        exit(0)
print 'Minecraft not yet launched. Giving up.'
exit(1)
