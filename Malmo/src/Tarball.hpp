/*
* tarball.hpp
*
*  Created on: Jul 28, 2010
*      Author: Pierre Lindenbaum PhD
*              plindenbaum@yahoo.fr
*              http://plindenbaum.blogspot.com
*
*
*		Modified by OJ in order to make cross platform
*       Modified by DB to increase buffer size and add files from memory
*/

#ifndef LINDENB_IO_TARBALL_HPP
#define LINDENB_IO_TARBALL_HPP

#include <iostream>
#include <exception>
#include <stdexcept>
#include <cstdio>
#include <cstring>
#include <cerrno>
#include <ctime>
#include <cstdio>
#include <cstdint>
#include <sstream>
#include <algorithm>

namespace lindenb { namespace io {
#define TARHEADER static_cast<PosixTarHeader*>(header)
	struct PosixTarHeader
	{
		char name[100];
		char mode[8];
		char uid[8];
		char gid[8];
		char size[12];
		char mtime[12];
		char checksum[8];
		char typeflag[1];
		char linkname[100];
		char magic[6];
		char version[2];
		char uname[32];
		char gname[32];
		char devmajor[8];
		char devminor[8];
		char prefix[155];
		char pad[12];
	};

	/**
	*  A Tar Archive
	*/

	class Tar
	{
	private:
		bool _finished;
		std::string _user_name;
		void checkSizeOfTagHeader()
		{
			if(sizeof(PosixTarHeader)!=512)
			{
				std::stringstream err;
				err << sizeof(PosixTarHeader);
				throw std::runtime_error(err.str());
			}
		}
	protected:
		std::ostream& out;
		void _init(void* header)
		{
			std::memset(header,0,sizeof(PosixTarHeader));
			std::sprintf(TARHEADER->magic,"ustar");
			std::sprintf(TARHEADER->mtime,"%011lo",(unsigned long)time(NULL));
			std::sprintf(TARHEADER->mode,"%07o",0644);
			if(!_user_name.empty())  std::sprintf(TARHEADER->uname,"%s",_user_name.c_str());
			std::sprintf(TARHEADER->gname,"%s","users");
		}
		void _checksum(void* header)
		{
			unsigned int sum = 0;
			char *p = (char *) header;
			char *q = p + sizeof(PosixTarHeader);
			while (p < TARHEADER->checksum) sum += *p++ & 0xff;
			for (int i = 0; i < 8; ++i)  {
				sum += ' ';
				++p;
			}
			while (p < q) sum += *p++ & 0xff;

			std::sprintf(TARHEADER->checksum,"%06o",sum);
		}
		void _size(void* header, unsigned long long fileSize)
		{
			std::sprintf(TARHEADER->size,"%011llo", fileSize);
		}
		void _filename(void* header,const char* filename)
		{
			if(filename==NULL || filename[0]==0 || std::strlen(filename)>=100)
			{
				std::stringstream err;
				err << "invalid archive name \"" << filename << "\"";
				throw std::runtime_error(err.str());
			}
			strcpy(TARHEADER->name,filename);
		}
		void _endRecord(std::size_t len)
		{
			char c='\0';
			while((len%sizeof(PosixTarHeader))!=0)
			{
				out.write(&c,sizeof(char));
				++len;
			}
		}
	public:
		Tar(std::ostream& out_, const std::string & user_name) : out(out_)
		{
			checkSizeOfTagHeader();
			if (user_name.length() <= 32)
			{
				_user_name = user_name;
			}
		}
		Tar(std::ostream& out_) : out(out_)
		{
			checkSizeOfTagHeader();
		}
		virtual ~Tar()
		{
			if(!_finished)
			{
				std::cerr << "[warning]tar file was not finished."<< std::endl;
			}
		}
		/** writes 2 empty blocks. Should be always called before closing the Tar file */
		void finish()
		{
			_finished=true;
			//The end of the archive is indicated by two blocks filled with binary zeros
			PosixTarHeader header;
			std::memset((void*)&header,0,sizeof(PosixTarHeader));
			out.write((const char*)&header,sizeof(PosixTarHeader));
			out.write((const char*)&header,sizeof(PosixTarHeader));
			out.flush();
		}
		void put(const char* filename,const std::string& s)
		{
			put(filename,s.c_str(),s.size());
		}
		void put(const char* filename,const char* content)
		{
			put(filename,content,std::strlen(content));
		}
		void put(const char* filename,const char* content,std::size_t len)
		{
			PosixTarHeader header;
			_init((void*)&header);
			_filename((void*)&header,filename);
			header.typeflag[0]=0;
			_size((void*)&header, (unsigned long long)len);
			_checksum((void*)&header);
			out.write((const char*)&header,sizeof(PosixTarHeader));
			out.write(content,len);
			_endRecord(len);
		}

		void putFile(const char* filename,const char* nameInArchive)
		{
			char buff[128 << 10]; // 128k
			std::FILE* in=std::fopen(filename,"rb");
			if(in==NULL)
			{
				std::stringstream err;
				err << "Cannot open " << filename << " "<< std::strerror(errno);
				throw std::runtime_error(err.str());
			}
			std::fseek(in, 0L, SEEK_END);
			long int len= std::ftell(in);
			std::fseek(in,0L,SEEK_SET);

			PosixTarHeader header;
			_init((void*)&header);
			_filename((void*)&header,nameInArchive);
			header.typeflag[0]=0;
			_size((void*)&header,len);
			_checksum((void*)&header);
			out.write((const char*)&header,sizeof(PosixTarHeader));

			std::size_t nRead=0;
			while((nRead=std::fread(buff,sizeof(char),128<<10,in))>0)
			{
				out.write(buff,nRead);
			}
			std::fclose(in);

			_endRecord(len);
		}

        void putMem(const char* memory, std::size_t size, const char* nameInArchive)
        {
            PosixTarHeader header;
            _init((void*)&header);
            _filename((void*)&header, nameInArchive);
            header.typeflag[0] = 0;
            _size((void*)&header, size);
            _checksum((void*)&header);
            out.write((const char*)&header, sizeof(PosixTarHeader));
            out.write(memory, size);
            _endRecord(size);
        }

        void putMemWithHeader(const char* mem_header, std::size_t headersize, const char* mem_body, std::size_t bodysize, const char* nameInArchive)
        {
            PosixTarHeader header;
            _init((void*)&header);
            _filename((void*)&header, nameInArchive);
            header.typeflag[0] = 0;
            _size((void*)&header, headersize + bodysize);
            _checksum((void*)&header);
            out.write((const char*)&header, sizeof(PosixTarHeader));
            out.write(mem_header, headersize);
            out.write(mem_body, bodysize);
            _endRecord(headersize + bodysize);
        }
    };

}}
#endif // LINDENB_IO_TARBALL_HPP
