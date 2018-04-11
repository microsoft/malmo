#ifndef _XMLPARSEEXCEPTION_H_
#define _XMLPARSEEXCEPTION_H_

// STL:
#include <cstdlib>

namespace malmo {

    class XMLParseException : std::exception {
    public:
        XMLParseException(const std::string msg) {
            this->msg = msg;
        }

        virtual const char* what() const noexcept {
            return msg.c_str();
        }
    private:
        std::string msg;
    };
}
#endif

