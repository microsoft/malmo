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

// Header:
#include "TimestampedReward.h"

// Local:
#include "FindSchemaFile.h"

// Boost:
#include <boost/date_time/posix_time/posix_time.hpp>
#include <boost/date_time/posix_time/posix_time_io.hpp>

// STL:
#include <sstream>

namespace malmo
{
    TimestampedReward::TimestampedReward()
    {
    }

    TimestampedReward::TimestampedReward(float reward)
    {
        this->values[0] = static_cast<double>(reward);
    }

    TimestampedReward& TimestampedReward::createFromXML(boost::posix_time::ptime timestamp, std::string xml_string)
    {
        this->timestamp = timestamp;

        const bool validate = true;
        
        xml_schema::properties props;
        props.schema_location(xml_namespace, FindSchemaFile("MissionEnded.xsd"));

        xml_schema::flags flags = xml_schema::flags::dont_initialize;
        if( !validate )
            flags = flags | xml_schema::flags::dont_validate;

        std::istringstream iss(xml_string);
        std::unique_ptr<malmo::schemas::Reward> reward = malmo::schemas::Reward_(iss, flags, props);
        setValuesFromRewardStructure(*reward);
        return *this;
    }

    TimestampedReward& TimestampedReward::createFromSimpleString(boost::posix_time::ptime timestamp, std::string simple_string)
    {
        this->timestamp = timestamp;

        // String should be comma-delimited sets of <dimension>:<value>.
        size_t nextpos = 0, lastpos = 0;
        while (nextpos != std::string::npos)
        {
            nextpos = simple_string.find(",", lastpos);
            std::string token = (nextpos != std::string::npos) ? simple_string.substr(lastpos, nextpos - lastpos) : simple_string.substr(lastpos);
            size_t split = token.find(":");
            if (split == std::string::npos)
            {
                throw std::runtime_error("Malformed reward message.");
            }
            else
            {
                int dimension = std::stoi(token.substr(0, split));
                double value = std::stod(token.substr(split + 1));
                this->values[dimension] = value;
            }
            lastpos = nextpos + 1;
        }
        return *this;
    }

    TimestampedReward::TimestampedReward(boost::posix_time::ptime timestamp,const schemas::Reward& reward)
        : timestamp(timestamp)
    {
        setValuesFromRewardStructure(reward);
    }
    
    void TimestampedReward::setValuesFromRewardStructure(const schemas::Reward& reward)
    {
        this->values.clear();
        for( const schemas::Value& r : reward.Value() ) {
            this->values[ r.dimension() ] = static_cast<double>( r.value() );
        }
    }
    
    schemas::Reward TimestampedReward::getAsRewardStructure() const
    {
        schemas::Reward reward;
        for( std::map<int,double>::const_iterator it = this->values.begin(); it!= this->values.end(); it++) {
            schemas::Value value( it->first, it->second );
            reward.Value().push_back( value );
        }
        return reward;
    }
    
    std::string TimestampedReward::getAsXML( bool prettyPrint ) const
    {
        std::ostringstream oss;
        
        xml_schema::namespace_infomap map;
        map[""].name = xml_namespace;
        map[""].schema = "MissionEnded.xsd";

        xml_schema::flags flags = xml_schema::flags::dont_initialize;
        if( !prettyPrint )
            flags = flags | xml_schema::flags::dont_pretty_print;

        Reward_( oss, this->getAsRewardStructure(), map, "UTF-8", flags );
        
        return oss.str();
    }

    std::string TimestampedReward::getAsSimpleString() const
    {
        std::ostringstream oss;
        for (std::map<int, double>::const_iterator it = this->values.begin(); it != this->values.end(); it++) {
            if (it != this->values.begin())
                oss << ",";
            oss << it->first << ":" << it->second;
        }
        return oss.str();
    }

    bool TimestampedReward::hasValueOnDimension(int dimension) const
    {
        return this->values.find(dimension) != this->values.end();
    }

    double TimestampedReward::getValueOnDimension(int dimension) const
    {
        return this->values.at(dimension);
    }

    double TimestampedReward::getValue() const
    {
        return this->values.at(0);
    }
    
    void TimestampedReward::add(const TimestampedReward& other)
    {
        for( std::map<int,double>::const_iterator it = other.values.begin(); it!= other.values.end(); it++) {
            int dimension = it->first;
            double value = it->second;
            if( this->values.find(dimension) != this->values.end() )
                this->values[dimension] += value;
            else
                this->values[dimension] = value;
        }
    }

    std::ostream& operator<<(std::ostream& os, const TimestampedReward& tsf)
    {
        os << "TimestampedReward: " << to_simple_string(tsf.timestamp);
        for( std::map<int,double>::const_iterator it = tsf.values.begin(); it!= tsf.values.end(); it++) {
            os  << ", " << it->first << ":" << it->second;
        }
        return os;
    }
}
