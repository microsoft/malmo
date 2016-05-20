// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

// Local:
#include "MissionRecord.h"

// Boost:
#include <boost/iostreams/copy.hpp>
#include <boost/iostreams/filter/gzip.hpp>
#include <boost/iostreams/filtering_streambuf.hpp>
#include <boost/iostreams/stream.hpp>
#include <boost/uuid/uuid.hpp>
#include <boost/uuid/uuid_generators.hpp>
#include <boost/uuid/uuid_io.hpp>

// STL:
#include <exception>
#include <iostream>

namespace malmo
{
    MissionRecord::MissionRecord(const MissionRecordSpec& spec)
        : spec(spec)
    {
        this->is_closed = false;
        if (spec.is_recording) {
            boost::filesystem::create_directories(this->spec.temp_dir);
        }
    }

    MissionRecord::~MissionRecord()
    {
        try {
            if (!this->is_closed) {
                this->close();
            }

        }
        catch (const std::exception& e) {
            // we don't really want to assume that is safe to write to cout but can't have destructors throwing exceptions
            std::cout << "Exception in closing of MissionRecord: " << e.what() << std::endl;
        }
        catch(...) {
            // we don't really want to assume that is safe to write to cout but can't have destructors throwing exceptions
            std::cout << "Unknown exception in closing of MissionRecord." << std::endl;
        }
    }

    MissionRecord::MissionRecord(MissionRecord&& record)
        : is_closed(record.is_closed)
        , spec(record.spec)
    {
        record.spec = MissionRecordSpec();
    }

    MissionRecord& MissionRecord::operator=(MissionRecord&& record)
    {
        if (this != &record){
            this->is_closed = record.is_closed;
            this->spec = record.spec;

            record.spec = MissionRecordSpec();
        }

        return *this;
    }

    void MissionRecord::close()
    {
        if (!this->spec.is_recording || this->is_closed){
            return;
        }

        // create zip file, push to destination
        std::vector<boost::filesystem::path> fileList;
        this->addFiles(fileList, this->spec.temp_dir);

        if (fileList.size() > 0){
            std::stringstream out("tempfile");
            lindenb::io::Tar tarball(out);

            for (auto file : fileList){
                try{
                    this->addFile(tarball, file);
                }
                catch (const std::exception& e){
                    std::cout << "[warning] Unable to archive " << file.string() << ": " << e.what() << std::endl;
                }
            }

            tarball.finish();
            {
                std::ofstream file(this->spec.destination, std::ofstream::binary);
                if (file.fail()) {
                    std::cout << "[warning] Unable to write recording to output file " << this->spec.destination << std::endl;
                }
                else {
                    boost::iostreams::filtering_streambuf< boost::iostreams::input> in;
                    in.push(boost::iostreams::gzip_compressor());
                    in.push(out);
                    boost::iostreams::copy(in, file);
                }
            }
        }

        boost::filesystem::remove_all(this->spec.temp_dir);

        this->is_closed = true;
    }

    void MissionRecord::addFiles(std::vector<boost::filesystem::path> &fileList, boost::filesystem::path directory)
    {
        if (!boost::filesystem::exists(directory))
        {
            throw std::runtime_error("Attempt to write to non-existent directory: " + directory.string());
        }
        boost::filesystem::directory_iterator dirIter(directory);
        boost::filesystem::directory_iterator dirIterEnd;

        while (dirIter != dirIterEnd)
        {
            if (boost::filesystem::exists(*dirIter))
            {
                if (boost::filesystem::is_directory(*dirIter)){
                    this->addFiles(fileList, *dirIter);
                }
                else
                {
                    fileList.push_back((*dirIter));
                }
            }
        
            ++dirIter;
        }
    }

    void MissionRecord::addFile(lindenb::io::Tar& archive, boost::filesystem::path path)
    {
        std::string file_name_in_archive = path.relative_path().normalize().string();
        std::replace(file_name_in_archive.begin(), file_name_in_archive.end(), '\\', '/');
        file_name_in_archive = file_name_in_archive.substr(2, file_name_in_archive.size());
        archive.putFile(path.string().c_str(), file_name_in_archive.c_str());
    }

    bool MissionRecord::isRecording() const
    {
        return this->spec.is_recording;
    }

    bool MissionRecord::isRecordingMP4() const
    {
        return this->spec.is_recording_mp4;
    }

    bool MissionRecord::isRecordingObservations() const
    {
        return this->spec.is_recording_observations;
    }

    bool MissionRecord::isRecordingRewards() const
    {
        return this->spec.is_recording_rewards;
    }

    bool MissionRecord::isRecordingCommands() const
    {
        return this->spec.is_recording_commands;
    }

    std::string MissionRecord::getMP4Path() const
    {
        return this->spec.mp4_path;
    }

    int64_t MissionRecord::getMP4BitRate() const
    {
        return this->spec.mp4_bit_rate;
    }

    int MissionRecord::getMP4FramesPerSecond() const
    {
        return this->spec.mp4_fps;
    }

    std::string MissionRecord::getObservationsPath() const
    {
        return this->spec.observations_path;
    }

    std::string MissionRecord::getRewardsPath() const
    {
        return this->spec.rewards_path;
    }

    std::string MissionRecord::getCommandsPath() const
    {
        return this->spec.commands_path;
    }

    std::string MissionRecord::getMissionInitPath() const
    {
        return this->spec.mission_init_path;
    }
}
