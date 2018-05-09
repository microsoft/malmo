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

'''Tests malmoutils.'''

import MalmoPython
import malmoutils

agentHost = MalmoPython.AgentHost()

# See if we can parse our extended command line.
malmoutils.parse_command_line(agentHost)

# As we are not recording our video xml should be an empty string.
assert malmoutils.get_video_xml(agentHost) == ''

# Test that we can get a default recording spec.
assert type(malmoutils.get_default_recording_object(agentHost, "test")) == MalmoPython.MissionRecordSpec

# Default recordings directory is ''.
assert malmoutils.get_recordings_directory(agentHost) == ''

# Test that a TrackingClientPool can indeed track some added ClientInfo objects.

clientPool = malmoutils.TrackingClientPool()
assert clientPool.getClientInfos() == []
client1 = MalmoPython.ClientInfo("localhost", 10000)
clientPool.add(client1)
assert clientPool.getClientInfos() == [client1]
client2 = MalmoPython.ClientInfo("localhost", 10001)
clientPool.add(client2)
assert clientPool.getClientInfos() == [client1, client2]

clientPool2 = malmoutils.TrackingClientPool([client1, client2])
assert clientPool.getClientInfos() == clientPool2.getClientInfos()

