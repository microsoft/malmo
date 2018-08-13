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
from lxml import etree
import argparse
import shutil


class ScoreLog:
    """Extract missions (digest, agent name) and their totals from score log files to given csv file."""
    def __init__(self, score_csv_file):
        self.score_file = score_csv_file
        self.separator = ","
        self.mission = ""
        self.agent = ""
        self.total = ""

    def new_mission(self, mission_init):
        self.mission = mission_init.find('MissionDigest').text
        self.agent = mission_init.find('AgentName').text

    def record_total(self, mission_total):
        self.total = mission_total.text
        self.output()

    def parse(self, message):
        # print(message)
        msg = etree.fromstring(message)
        if msg.tag == 'MissionInit':
            self.new_mission(msg)
        elif msg.tag == 'MissionTotal':
            self.record_total(msg)

    def score(self, file):
        try:
            log = etree.parse(file)
            root = log.getroot()
            for child in root:
                # print(child.tag)
                if child.tag == 'record':
                    self.parse(child.find('message').text)

        except etree.XMLSyntaxError as err:
            # Incomplete log files don't have a closing </log>.
            # Try to copy file, append and re-parse.
            print('XMLSyntaxError ' + str(err))
            if 'Premature end of data' in str(err):
                print('Re-try after appending </log>')
                file2 = file + '2'
                shutil.copyfile(file, file2)
                with open(file2, 'a') as log:
                    log.write('</log>')
                self.score(file2)
            else:
                raise

    def output(self):
        self.score_file.write(self.mission + self.separator + self.agent + self.separator + self.total + '\n')


if __name__ == '__main__':

    parser = argparse.ArgumentParser(description='score missions')
    parser.add_argument('--log_files', nargs='+', default=[], help='the log files to score')
    args = parser.parse_args()

    with open('score.csv', 'w') as score_file:
        [ScoreLog(score_file).score(log_file) for log_file in args.log_files]
