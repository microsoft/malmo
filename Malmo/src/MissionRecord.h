// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

#ifndef _MISSIONRECORD_H_
#define _MISSIONRECORD_H_

// Local:
#include "Tarball.hpp"
#include "MissionRecordSpec.h"

// Boost:
#include <boost/filesystem.hpp>

// STL:
#include <cstdint>
#include <fstream>
#include <string>
#include <vector>

namespace malmo
{
    //! Handles the mission record file.
    class MissionRecord
    {
        public:        
            //! Constructs a mission record handler with the provided specification
            MissionRecord(const MissionRecordSpec& spec);

            MissionRecord(MissionRecord&& record);
            MissionRecord& operator= (MissionRecord&& record);

            MissionRecord(const MissionRecord&) = delete;
            MissionRecord& operator= (const MissionRecord&) = delete;
            
            //! Finalizes the mission record, closing the record if that has not already been performed.
            ~MissionRecord();

            //! Saves the recording to file.
            //! Only call this once per instance of MissionRecord, after all the missions have finished,
            //! else the paths returned by e.g. getVideoPath() will be invalid.
            void close();

            //! Gets whether the mission will be recorded or not.
            //! \returns Boolean value.
            bool isRecording() const;

            //! Gets whether the video from the mission will be recorded or not.
            //! \returns Boolean value.
            bool isRecordingMP4() const;

            //! Gets whether the observations from the mission will be recorded or not.
            //! \returns Boolean value.
            bool isRecordingObservations() const;

            //! Gets whether the rewards from the mission will be recorded or not.
            //! \returns Boolean value.
            bool isRecordingRewards() const;

            //! Gets whether the commands sent during the mission will be recorded or not.
            //! \returns Boolean value.
            bool isRecordingCommands() const;

            //! Gets the path where the mp4 video should be saved to, if recording has been requested.
            //! \returns The path as a string.
            std::string getMP4Path() const;

            //! Gets the bitrate at which the video should be recorded, if MP4 recording has been requested.
            //! \returns The bitrate in bits per second.
            int64_t getMP4BitRate() const;

            //! Gets the frequency at which frames should be recorded, if MP4 recording has been requested.
            //! \returns The frames per second.
            int getMP4FramesPerSecond() const;

            //! Gets the path where the observations should be saved to, if recording has been requested.
            //! \returns The path as a string.
            std::string getObservationsPath() const;

            //! Gets the path where the rewards should be saved to, if recording has been requested.
            //! \returns The path as a string.
            std::string getRewardsPath() const;

            //! Gets the path where the commands should be saved to, if recording has been requested.
            //! \returns The path as a string.
            std::string getCommandsPath() const;

            //! Gets the path where the mission init should be saved to
            //! \returns The path as a string
            std::string getMissionInitPath() const;

        private:

            bool is_closed;
            MissionRecordSpec spec;

            void addFiles(std::vector<boost::filesystem::path> &fileList, boost::filesystem::path directory);
            void addFile(lindenb::io::Tar& archive, boost::filesystem::path path);
    };
}

#endif
