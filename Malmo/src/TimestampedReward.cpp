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
    TimestampedReward::TimestampedReward(boost::posix_time::ptime timestamp,std::string xml_string)
    {
        const bool validate = true;
        
        xml_schema::properties props;
        props.schema_location(xml_namespace, FindSchemaFile("MissionEnded.xsd"));

        xml_schema::flags flags = 0;
        if( !validate )
            flags = flags | xml_schema::flags::dont_validate;

        std::istringstream iss(xml_string);
        std::unique_ptr<malmo::schemas::Reward> reward = malmo::schemas::Reward_(iss, flags, props);
        setValuesFromRewardStructure(*reward);
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

        xml_schema::flags flags = 0;
        if( !prettyPrint )
            flags = flags | xml_schema::flags::dont_pretty_print;

        Reward_( oss, this->getAsRewardStructure(), map, "UTF-8", flags );
        
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
        for( std::map<int,double>::const_iterator it = this->values.begin(); it!= this->values.end(); it++) {
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
