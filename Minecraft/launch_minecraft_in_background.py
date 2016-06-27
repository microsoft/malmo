# ------------------------------------------------------------------------------------------------
# Copyright (c) 2016 Microsoft Corporation
# 
# Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
# associated documentation files (the "Software"), to deal in the Software without restriction,
# including without limitation the rights to use, copy, modify, merge, publish, distribute,
# sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
# 
# The above copyright notice and this permission notice shall be included in all copies or
# substantial portions of the Software.
# 
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
# NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
# NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
# DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
# ------------------------------------------------------------------------------------------------

# Used for integration tests.

import os
import platform
import socket
import subprocess
import sys
import time

print os.getcwd()

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
if os.name == 'nt':
    os.startfile("launchClient.bat")
elif sys.platform == 'darwin':
    subprocess.Popen(['open', '-a', 'Terminal.app', 'launchClient.sh'])
elif platform.linux_distribution()[0] == 'Fedora':
    subprocess.Popen( "gnome-terminal -e ./launchClient.sh", close_fds=True, shell=True )
else:
    subprocess.Popen( "x-terminal-emulator -e ./launchClient.sh", close_fds=True, shell=True )

print 'Giving Minecraft some time to launch... '
for i in xrange( 100 ):
    print '.',
    time.sleep( 3 )
    if PortHasListener(10000):
        print 'ok'
        exit(0)
print 'Minecraft not yet launched. Giving up.'
exit(1)
