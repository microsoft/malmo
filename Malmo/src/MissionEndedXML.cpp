// --------------------------------------------------------------------------------------------------
//  Copyright (c) 2016 Microsoft Corporation
//  
//  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
//  associated documentation files (the "Software"), to deal in the Software without restriction,
//  including without limitation the rights to use, copy, modify, merge, publish, distribute,
//  sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions:
//  
//  The above copyright notice and this permission notice shall be included in all copies or
//  substantial portions of the Software.
//  
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
//  NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
//  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
//  DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// --------------------------------------------------------------------------------------------------

#include "MissionEndedXML.h"
#include "MissionSpec.h"
#include "XMLParseException.h"

// STL:
#include <exception>
#include <iostream>

// Boost:
#include <boost/property_tree/xml_parser.hpp>

namespace malmo {

    MissionEndedXML::MissionEndedXML(std::string xml_text) {
        boost::property_tree::ptree xml;

        std::istringstream is(xml_text);
        boost::property_tree::read_xml(is, xml);

        schema_version = xml.get_optional<std::string>("MissionEnded.<xmlattr>.SchemaVersion");
        status = xml.get<std::string>("MissionEnded.Status");
        human_readable_status = xml.get<std::string>("MissionEnded.HumanReadableStatus");

        const auto& reward_element = xml.get_child_optional("MissionEnded.Reward");
        have_rewards = reward_element != boost::none;
        if (have_rewards) {
            reward.parse_rewards(reward_element.get());
            if (reward.size() == 0) {
                throw XMLParseException("Reward must have at least one value");
            }
        }

        for (boost::property_tree::ptree::value_type const& v : xml.get_child("MissionEnded.MissionDiagnostics")) {
            if (v.first == "VideoData") {
                VideoDataAttributes attributes;

                attributes.frame_type = v.second.get<std::string>("<xmlattr>.frameType");
                attributes.frames_sent = v.second.get<int>("<xmlattr>.framesSent");
                attributes.frames_received = v.second.get_optional<int>("<xmlattr>.framesReceived");
                attributes.frames_written = v.second.get_optional<int>("<xmlattr>.framesWritten");

                video_data_attributes.push_back(attributes);
            }
        }
    }

    std::string MissionEndedXML::toXml() const {
        std::ostringstream oss;

        boost::property_tree::ptree xml;

        xml.put("MissionEnded.<xmlattr>.xmlns", MissionSpec::MALMO_NAMESPACE);
        xml.put("MissionEnded.<xmlattr>.xmlns:xsi", MissionSpec::XMLNS_XSI);
        if (schema_version)
            xml.put("MissionEnded.<xmlattr>.SchemaVersion", schema_version);
        xml.put("MissionEnded.Status", status);
        xml.put("MissionEnded.HumanReadableStatus", human_readable_status);

        if (have_rewards)
            reward.add_rewards(xml.get_child("MissionEnded"));

        for (auto& d : video_data_attributes) {
            boost::property_tree::ptree videoData;
            videoData.put("<xmlattr>.frameType", d.frame_type);
            videoData.put("<xmlattr>.framesSent", d.frames_sent);
            xml.add_child("MissionEnded.MissionDiagnostics", videoData);
        }

        write_xml(oss, xml);

        std::string xml_str = oss.str();
        xml_str.erase(std::remove(xml_str.begin(), xml_str.end(), '\n'), xml_str.end());

        return xml_str;
    }
}


