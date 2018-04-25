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

#ifndef _TIMESTAMPEDREWARD_H_
#define _TIMESTAMPEDREWARD_H_

// Boost:
#include <boost/date_time/posix_time/posix_time_types.hpp>

// STL:
#include <map>

#include "RewardXML.h"

namespace malmo
{
    //! A map of int:double storing a value on each dimension, with an attached timestamp saying when it was collected.
    class TimestampedReward
    {
        public:
            //! Constructs an empty reward.
            TimestampedReward();

            //! Constructs from a single reward float (assumes default dimension of 0)
            TimestampedReward(float reward);

            //! Constructs from an XML string.
            TimestampedReward& createFromXML(boost::posix_time::ptime timestamp,std::string xml_string);

            //! Constructs from a simple string.
            TimestampedReward& createFromSimpleString(boost::posix_time::ptime timestamp, std::string simple_string);
            
            //! Constructs from an XML node element.
            TimestampedReward(boost::posix_time::ptime timestamp, const RewardXML& reward);

            //! Formats as an XML string.
            //! \param prettyPrint If true, add indentation and newlines to the XML to make it more readable.
            //! \returns The reward as an XML string.
            std::string getAsXML( bool prettyPrint ) const;

            //! Formats as a simple string.
            //! \returns The reward in simple string form.
            std::string getAsSimpleString() const;

            //! The timestamp.
            boost::posix_time::ptime timestamp;

            //! Returns whether a reward value is stored on the specified dimension.
            bool hasValueOnDimension(int dimension) const;
            
            //! Returns the reward value stored on the specified dimension.
            double getValueOnDimension(int dimension) const;
            
            //! Returns the reward value stored on dimension zero. By default the reward producers store their output here.
            double getValue() const;
            
            //! Merge the specified reward structure into this one, adding rewards that are on the same dimension.
            void add(const TimestampedReward& other);
            
            //! Stream a readable version, for casual inspection.
            friend std::ostream& operator<<(std::ostream& os, const TimestampedReward& tsf);
        
        private:

            RewardXML reward;
    };
}

#endif
