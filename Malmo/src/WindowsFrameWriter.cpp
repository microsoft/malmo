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
#include "WindowsFrameWriter.h"

// Boost:
#include <boost/filesystem.hpp>

// STL:
#include <exception>
#include <sstream>

namespace malmo
{
    WindowsFrameWriter::WindowsFrameWriter(std::string path, std::string info_filename, short width, short height, int frames_per_second, int64_t bit_rate, int channels, bool drop_input_frames)
        : VideoFrameWriter(path, info_filename, width, height, frames_per_second, channels, drop_input_frames)
        , bit_rate(bit_rate)
    {
        this->ffmpeg_path = search_path();
        if (this->ffmpeg_path.length() == 0) {
            throw std::runtime_error("FFMPEG not available.");
        }

        this->g_hChildStd_IN_Rd = NULL;
        this->g_hChildStd_IN_Wr = NULL;
        this->g_hChildStd_OUT_Wr = NULL;
    }

    void WindowsFrameWriter::open()
    {
        VideoFrameWriter::open();

        SECURITY_ATTRIBUTES saAttr;

        saAttr.nLength = sizeof(SECURITY_ATTRIBUTES);
        saAttr.bInheritHandle = TRUE;
        saAttr.lpSecurityDescriptor = NULL;

        // Create a pipe for the child process's STDIN. 
        if (!CreatePipe(&this->g_hChildStd_IN_Rd, &this->g_hChildStd_IN_Wr, &saAttr, 0))
        {
            throw std::runtime_error("Error creating pipe.");
        }

        // Ensure the write handle to the pipe for STDIN is not inherited. 
        if (!SetHandleInformation(this->g_hChildStd_IN_Wr, HANDLE_FLAG_INHERIT, 0))
        {
            throw std::runtime_error("Unable to set handle information on pipe.");
        }

        boost::filesystem::path fs_path(this->path);
        std::string ffmpeg_outfile = (fs_path.parent_path() / (fs_path.stem().string() + "_ffmpeg.out")).string();
        this->g_hChildStd_OUT_Wr = CreateFile(ffmpeg_outfile.c_str(),
            GENERIC_WRITE,
            FILE_SHARE_WRITE | FILE_SHARE_READ,
            &saAttr,
            OPEN_ALWAYS,
            FILE_ATTRIBUTE_NORMAL,
            NULL);

        this->ffmpeg_thread = boost::thread(&WindowsFrameWriter::runFFMPEG, this);
    }

    WindowsFrameWriter::~WindowsFrameWriter()
    {
        this->close();
    }

    void WindowsFrameWriter::close()
    {
        if (this->is_open)
        {
            VideoFrameWriter::close();

            if (!CloseHandle(g_hChildStd_IN_Wr))
                throw std::runtime_error("Unable to close pipe.");

            this->ffmpeg_thread.join();

            if (!CloseHandle(g_hChildStd_OUT_Wr))
                throw std::runtime_error("Unable to close output file.");
        }
    }

    void WindowsFrameWriter::doWrite(char* rgb, int width, int height, int frame_index)
    {
        DWORD dwWritten;
        BOOL bSuccess = FALSE;
        std::stringstream header;
        std::string magic_number = this->channels == 1 ? "P5" : "P6";
        header << magic_number << "\n" << width << " " << height << "\n255\n";
        std::string headerStr = header.str();        

        bSuccess = WriteFile(this->g_hChildStd_IN_Wr, headerStr.c_str(), (DWORD)headerStr.length(), &dwWritten, NULL);
        if (!bSuccess){
            throw std::runtime_error("Unable to write frame header to pipe.");
        }

        bSuccess = WriteFile(this->g_hChildStd_IN_Wr, rgb, width*height*this->channels, &dwWritten, NULL);
        if (!bSuccess){
            throw std::runtime_error("Unable to write frame pixels to pipe.");
        }
    }

    std::string WindowsFrameWriter::search_path()
    {
        std::string path = ::getenv("PATH");
        if (path.empty())
        {
            throw std::runtime_error("Environment variable PATH not found");
        }

        std::stringstream path_stream(path);
        std::string folder_path;

        while (std::getline(path_stream, folder_path, ';')) {
            boost::filesystem::path executable_path = folder_path;
            executable_path /= "ffmpeg.exe";

            boost::system::error_code ec;
            bool file = boost::filesystem::is_regular_file(executable_path, ec);
            if (!ec && file && SHGetFileInfoA(executable_path.string().c_str(), 0, 0, 0, SHGFI_EXETYPE))
            {
                return executable_path.string();
            }
        }

        return "";
    }

    void WindowsFrameWriter::runFFMPEG()
    {
        PROCESS_INFORMATION piProcInfo;
        STARTUPINFO siStartInfo;
        BOOL bSuccess = FALSE;

        std::string input_format = this->channels == 1 ? "pgm" : "ppm";
        std::stringstream cmd_line;
        cmd_line << this->ffmpeg_path << " -y -f image2pipe "
            << "-framerate " << this->frames_per_second
            //<< "-s " << this->width << "x" << this->height
            << " -c:v " << input_format << " -i - -c:v libx264 "
            << "-b:v " << this->bit_rate
            << " -pix_fmt yuv420p " << this->path;

        // Set up members of the PROCESS_INFORMATION structure. 

        ZeroMemory(&piProcInfo, sizeof(PROCESS_INFORMATION));

        // Set up members of the STARTUPINFO structure. 
        // This structure specifies the STDIN and STDOUT handles for redirection.

        ZeroMemory(&siStartInfo, sizeof(STARTUPINFO));
        siStartInfo.cb = sizeof(STARTUPINFO);
        siStartInfo.hStdError = this->g_hChildStd_OUT_Wr;
        siStartInfo.hStdOutput = this->g_hChildStd_OUT_Wr;
        siStartInfo.hStdInput = this->g_hChildStd_IN_Rd;
        siStartInfo.dwFlags |= STARTF_USESTDHANDLES;

        // Create the child process. 

        LPSTR szCmdline = _strdup(cmd_line.str().c_str());

        bSuccess = CreateProcess(NULL,
            szCmdline,        // command line 
            NULL,             // process security attributes 
            NULL,             // primary thread security attributes 
            TRUE,             // handles are inherited 
            CREATE_NO_WINDOW, // creation flags 
            NULL,             // use parent's environment 
            NULL,             // use parent's current directory 
            &siStartInfo,     // STARTUPINFO pointer 
            &piProcInfo);     // receives PROCESS_INFORMATION 

        // Wait until child process exits.
        WaitForSingleObject(piProcInfo.hProcess, INFINITE);

        // Close process and thread handles. 
        CloseHandle(piProcInfo.hProcess);
        CloseHandle(piProcInfo.hThread);

        free(szCmdline);
    }
}
