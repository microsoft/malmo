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
    MissionRecord::MissionRecord(const MissionRecordSpec& spec) : spec(spec)
    {
        if (spec.isRecording()) {
            boost::uuids::random_generator gen;
            boost::uuids::uuid temp_uuid = gen();
            char *malmo_tmp_path = getenv("MALMO_TEMP_PATH");
            if (malmo_tmp_path)
                this->temp_dir = boost::filesystem::path(malmo_tmp_path);
            else
                this->temp_dir = boost::filesystem::path(".");
            this->mission_id = boost::uuids::to_string(temp_uuid);
            this->temp_dir = this->temp_dir / "mission_records" / this->mission_id;
            this->mp4_path = (this->temp_dir / "video.mp4").string();
            this->mp4_depth_path = (this->temp_dir / "depth_video.mp4").string();
            this->mp4_luminance_path = (this->temp_dir / "luminance_video.mp4").string();
            this->mp4_colourmap_path = (this->temp_dir / "colourmap_video.mp4").string();
            this->observations_path = (this->temp_dir / "observations.txt").string();
            this->rewards_path = (this->temp_dir / "rewards.txt").string();
            this->commands_path = (this->temp_dir / "commands.txt").string();
            this->mission_init_path = (this->temp_dir / "missionInit.xml").string();
            bool created_tmp = false;
            try {
                created_tmp = boost::filesystem::create_directories(this->temp_dir);
            }
            catch (const std::exception& e) {
                std::cout << "Unable to create temporary folder for recording " << this->temp_dir.string() << ": " << e.what() << std::endl;
                throw std::runtime_error("Check your MALMO_TEMP_PATH and try again.");
            }

            if (created_tmp) {
                this->is_closed = false;
            }
            else {
                throw std::runtime_error("Unable to create temporary folder for recording " + this->temp_dir.string() + ": check your MALMO_TEMP_PATH?");
            }
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
        , commands_path(record.commands_path)
        , mp4_path(record.mp4_path)
        , mp4_depth_path(record.mp4_depth_path)
        , mp4_luminance_path(record.mp4_luminance_path)
        , mp4_colourmap_path(record.mp4_colourmap_path)
        , observations_path(record.observations_path)
        , rewards_path(record.rewards_path)
        , mission_init_path(record.mission_init_path)
        , temp_dir(record.temp_dir)
        , mission_id(record.mission_id)
    {
        record.spec = MissionRecordSpec();
    }

    MissionRecord& MissionRecord::operator=(MissionRecord&& record)
    {
        if (this != &record){
            this->is_closed = record.is_closed;
            this->spec = record.spec;
            this->commands_path = record.commands_path;
            this->mp4_path = record.mp4_path;
            this->mp4_depth_path = record.mp4_depth_path;
            this->mp4_luminance_path = record.mp4_luminance_path;
            this->mp4_colourmap_path = record.mp4_colourmap_path;
            this->observations_path = record.observations_path;
            this->rewards_path = record.rewards_path;
            this->mission_init_path = record.mission_init_path;
            this->temp_dir = record.temp_dir;
            this->mission_id = record.mission_id;

            record.spec = MissionRecordSpec();
        }

        return *this;
    }

    void MissionRecord::close()
    {
        if (!this->spec.isRecording() || this->is_closed){
            return;
        }

        // create zip file, push to destination
        std::vector<boost::filesystem::path> fileList;
        this->addFiles(fileList, this->temp_dir);

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

        boost::filesystem::remove_all(this->temp_dir);

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
        // boost::filesystem::relative would do what we want here, but it wasn't introduced until boost 1.60, and
        // we still want to support operating systems with older versions.
        boost::filesystem::path filepath = boost::filesystem::absolute(path);
        boost::filesystem::path tempdirpath = boost::filesystem::absolute(this->temp_dir);
        boost::filesystem::path::iterator it_file = filepath.begin();
        boost::filesystem::path::iterator it_tmpdir = tempdirpath.begin();
        boost::filesystem::path relpath = this->mission_id; // Start with the mission_id as our root.
        // Skip everything which is in both paths:
        while (*it_file == *it_tmpdir && it_file != filepath.end() && it_tmpdir != tempdirpath.end())
        {
            it_file++, it_tmpdir++;
        }
        // Now get rest of file path:
        for (; it_file != filepath.end(); it_file++)
        {
            relpath /= *it_file;
        }
        std::string file_name_in_archive = relpath.normalize().string();
        std::replace(file_name_in_archive.begin(), file_name_in_archive.end(), '\\', '/');
        archive.putFile(path.string().c_str(), file_name_in_archive.c_str());
    }

    bool MissionRecord::isRecording() const
    {
        return this->spec.isRecording();
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
        return this->mp4_path;
    }

    std::string MissionRecord::getMP4DepthPath() const
    {
        return this->mp4_depth_path;
    }

    std::string MissionRecord::getMP4LuminancePath() const
    {
        return this->mp4_luminance_path;
    }

    std::string MissionRecord::getMP4ColourMapPath() const
    {
        return this->mp4_colourmap_path;
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
        return this->observations_path;
    }

    std::string MissionRecord::getRewardsPath() const
    {
        return this->rewards_path;
    }

    std::string MissionRecord::getCommandsPath() const
    {
        return this->commands_path;
    }

    std::string MissionRecord::getMissionInitPath() const
    {
        return this->mission_init_path;
    }

    std::string MissionRecord::getTemporaryDirectory() const
    {
        if (!this->spec.isRecording()){
            throw std::runtime_error("Mission is not being recorded.");
        }

        if (boost::filesystem::exists(this->temp_dir)){
            return this->temp_dir.string();
        }
        else{
            throw std::runtime_error("Mission record does not yet exist. Temporary directory will be created once a mission has begun.");
        }
    }
}
