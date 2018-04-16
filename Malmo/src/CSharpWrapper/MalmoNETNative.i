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

%module MalmoNETNative
%{
  // Malmo:
  #include <AgentHost.h>
  #include <ClientPool.h>
  #include <MissionSpec.h>
  #include <ParameterSet.h>
  #include <Logger.h>
  using namespace malmo;
  
  // Boost:
  #include <boost/date_time/posix_time/posix_time.hpp>
  
  // STL:
  #include <sstream>
%}

#ifdef WRAP_ALE
%{
  #include <ALEAgentHost.h>
%}
#endif

%include "std_string.i"
%include "std_vector.i"
%include "boost_shared_ptr.i"
%include "stdint.i"

%shared_ptr(TimestampedVideoFrame)
%shared_ptr(TimestampedReward)
%shared_ptr(TimestampedString)

%template(TimestampedVideoFramePtr) boost::shared_ptr< TimestampedVideoFrame >;
%template(TimestampedRewardPtr)     boost::shared_ptr< TimestampedReward >;
%template(TimestampedStringPtr)     boost::shared_ptr< TimestampedString >;

%template(StringVector)                std::vector< std::string >;
%template(TimestampedVideoFrameVector) std::vector< boost::shared_ptr< TimestampedVideoFrame > >;
%template(TimestampedRewardVector)     std::vector< boost::shared_ptr< TimestampedReward > >;
%template(TimestampedStringVector)     std::vector< boost::shared_ptr< TimestampedString > >;
%template(ByteVector)                  std::vector<unsigned char>;

namespace boost::posix_time
{
  class ptime
  {
  public:
    ptime();
    ptime(ptime time);
  };
  
  std::string to_simple_string(ptime timestamp);

  %typemap(cstype, out="System.DateTime") ptime * "ref System.DateTime"

  %typemap(csvarout, excode=SWIGEXCODE2) ptime * %{
      /* csvarout typemap code */
      get {
        global::System.IntPtr cPtr = $imcall;
        ptime tempDate = (cPtr == global::System.IntPtr.Zero) ? null : new ptime(cPtr, $owner);$excode
        return global::System.DateTime.Parse(MalmoNETNative.to_simple_string(tempDate));
      } %}
}

class Logger
{
public:
  enum LoggingSeverityLevel { 
    LOG_OFF
    , LOG_ERRORS
    , LOG_WARNINGS
    , LOG_INFO
    , LOG_FINE
    , LOG_TRACE
    , LOG_ALL
  };

    static void setLogging(const std::string& destination, Logger::LoggingSeverityLevel level);
    static void appendToLog(Logger::LoggingSeverityLevel level, const std::string& message);
};

// We want to throw custom MissionException objects which are derived from System.ApplicationException:
%typemap(csbase) MissionException "System.ApplicationException";

// There's a fair amount of hoop-jumping in order to make the custom exceptions work...
%insert(runtime) %{
  // Code to handle throwing of C# CustomApplicationException from C/C++ code.
  // The equivalent delegate to the callback, CSharpExceptionCallback_t, is CustomExceptionDelegate
  // and the equivalent customExceptionCallback instance is customDelegate
  typedef void (SWIGSTDCALL* CSharpExceptionCallback_t)(const char *, int);
  CSharpExceptionCallback_t customExceptionCallback = NULL;

  extern "C" SWIGEXPORT
  void SWIGSTDCALL CustomExceptionRegisterCallback(CSharpExceptionCallback_t customCallback) {
    customExceptionCallback = customCallback;
  }

  // Note that SWIG detects any method calls named starting with
  // SWIG_CSharpSetPendingException for warning 845
  static void SWIG_CSharpSetPendingExceptionCustom(const char *msg, int code) {
    customExceptionCallback(msg, code);
  }
%}

%pragma(csharp) imclasscode=%{
  class CustomExceptionHelper {
    // C# delegate for the C/C++ customExceptionCallback
    public delegate void CustomExceptionDelegate(string message, MissionException.MissionErrorCode code);
    static CustomExceptionDelegate customDelegate = new CustomExceptionDelegate(SetPendingCustomException);

    [global::System.Runtime.InteropServices.DllImport("$dllimport", EntryPoint="CustomExceptionRegisterCallback")]
    public static extern void CustomExceptionRegisterCallback(CustomExceptionDelegate customCallback);

    static void SetPendingCustomException(string message, MissionException.MissionErrorCode code) {
      SWIGPendingException.Set(new MissionException(message, code, false)); // Third parameter unused - only provided to force use of correct constructor - see comments below.
    }

    static CustomExceptionHelper() {
      CustomExceptionRegisterCallback(customDelegate);
    }
  }
  static CustomExceptionHelper exceptionHelper = new CustomExceptionHelper();
%}

%typemap(throws, canthrow=1) MissionException {
  SWIG_CSharpSetPendingExceptionCustom($1.what(), $1.getMissionErrorCode());
  return $null;
}

// If we leave things as is, we'll get a nice custom MissionException object being thrown, which wraps our C++ MissionException object,
// but the C# base-class (System.ApplicationException) won't get explicitly called with the messsage parameter.
// This isn't the end of the world, but it means that a default "something went wrong with the application" message will be used instead,
// and the user will have to call MissionException.getMessage() to get the actual error message. Any existing usercode which just
// uses "catch(Exception)" rather than "catch(MissionException)" will lose the information they were previously getting.
// In order to make this work properly, we use the cscode typemap to add a new pair of MissionException constructors which will pass the message parameter to the base-class.
// The complication with this approach is that we also need to SWIG the normal constructor, otherwise SWIG won't generate the CSharp_new_MissionException(char*, int)
// which we need to construct the C# MissionException object at all. I couldn't find a way to pursuade SWIG to generate just the IM code and not the C# method.
// So to disambiguate between the two sets of constructors, we add a dummy parameter to our own constructor, and make sure our SetPendingCustomException method
// calls the correct one.

%typemap(cscode) MissionException %{
  internal MissionException(global::System.IntPtr cPtr, bool cMemoryOwn, string message) : base(message) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = new global::System.Runtime.InteropServices.HandleRef(this, cPtr);
  }

  public MissionException(string message, MissionException.MissionErrorCode code, bool dummyParameter) : this(MalmoNETNativePINVOKE.new_MissionException(message, (int)code), true, message) {
    if (MalmoNETNativePINVOKE.SWIGPendingException.Pending) throw MalmoNETNativePINVOKE.SWIGPendingException.Retrieve();
  }
%}

class MissionException : public std::exception
{
public:
    enum MissionErrorCode
    {
        MISSION_BAD_ROLE_REQUEST,
        MISSION_BAD_VIDEO_REQUEST,
        MISSION_ALREADY_RUNNING,
        MISSION_INSUFFICIENT_CLIENTS_AVAILABLE,
        MISSION_TRANSMISSION_ERROR,
        MISSION_SERVER_WARMING_UP,
        MISSION_SERVER_NOT_FOUND,
        MISSION_NO_COMMAND_PORT,
        MISSION_BAD_INSTALLATION,
        MISSION_CAN_NOT_KILL_BUSY_CLIENT,
        MISSION_CAN_NOT_KILL_IRREPLACEABLE_CLIENT
    };
    MissionException(const std::string& message, MissionErrorCode code);    // Need this to get the underlying new_MissionException code from SWIG.
    ~MissionException();
    MissionErrorCode getMissionErrorCode() const;
    std::string getMessage() const;
};

class MissionRecordSpec
{
public:
    MissionRecordSpec();
    MissionRecordSpec(std::string destination);
    void recordMP4(int frames_per_second, int64_t bit_rate);
    void recordMP4(TimestampedVideoFrame::FrameType type, int frames_per_second, int64_t bit_rate, bool drop_input_frames);
    void recordBitmaps(TimestampedVideoFrame::FrameType type);
    void recordObservations();
    void recordRewards();
    void recordCommands();
    void setDestination(const std::string& destination);
};

class ArgumentParser
{
public:
  ArgumentParser(const std::string& title);

  %exception parse(const std::vector< std::string >& args) %{
    try {
      $action
    } catch (boost::program_options::invalid_command_line_syntax& e) {
      SWIG_CSharpSetPendingException(SWIG_CSharpApplicationException, e.what());
    } catch (std::exception& e) {
      SWIG_CSharpSetPendingException(SWIG_CSharpApplicationException, e.what());
    }
  %}

  void parse(const std::vector< std::string >& args);

  void addOptionalIntArgument(const std::string& name, const std::string& description, int defaultValue);

  void addOptionalFloatArgument(const std::string& name, const std::string& description, double defaultValue);

  void addOptionalStringArgument(const std::string& name, const std::string& description, const std::string& defaultValue);

  void addOptionalFlag(const std::string& name, const std::string& description);

  std::string getUsage() const;

  bool receivedArgument(const std::string& name) const;

  int getIntArgument(const std::string& name) const;

  double getFloatArgument(const std::string& name) const;

  std::string getStringArgument(const std::string& name) const;
};

class WorldState
{
public:
  const bool is_mission_running;

  const bool has_mission_begun;

  const int number_of_video_frames_since_last_state;

  const int number_of_rewards_since_last_state;

  const int number_of_observations_since_last_state;

  const std::vector< boost::shared_ptr< TimestampedVideoFrame > > video_frames;

  const std::vector< boost::shared_ptr< TimestampedReward > > rewards;

  const std::vector< boost::shared_ptr< TimestampedString > > observations;

  const std::vector< boost::shared_ptr< TimestampedString > > mission_control_messages;
  
  const std::vector< boost::shared_ptr< TimestampedString > > errors;
};

class AgentHost : public ArgumentParser {
public:
  enum VideoPolicy { 
    LATEST_FRAME_ONLY          
  , KEEP_ALL_FRAMES            
  };

  enum RewardsPolicy { 
    LATEST_REWARD_ONLY         
  , SUM_REWARDS                
  , KEEP_ALL_REWARDS           
  };

  enum ObservationsPolicy { 
    LATEST_OBSERVATION_ONLY    
  , KEEP_ALL_OBSERVATIONS      
  };

  AgentHost();

  void startMission(
      const MissionSpec& mission
    , const ClientPool& client_pool
    , const MissionRecordSpec& mission_record
    , int role
    , std::string unique_experiment_id
  ) throw(MissionException);

  void startMission(
      const MissionSpec& mission
    , const MissionRecordSpec& mission_record
  ) throw(MissionException);

  bool killClient(const ClientInfo& client);

  WorldState peekWorldState() const;
  
  WorldState getWorldState();

  void setVideoPolicy(VideoPolicy videoPolicy);

  void setRewardsPolicy(RewardsPolicy rewardsPolicy);

  void setObservationsPolicy(ObservationsPolicy observationsPolicy);

  void sendCommand(std::string command);

  void sendCommand(std::string command, std::string key);

  std::string getRecordingTemporaryDirectory();

  void setDebugOutput(bool debug);
};

#ifdef WRAP_ALE
class ALEAgentHost : public ArgumentParser {
public:

  ALEAgentHost();

  %exception startMission(
      const MissionSpec& mission
    , const ClientPool& client_pool
    , const MissionRecordSpec& mission_record
    , int role
    , std::string unique_experiment_id
  ) %{
    try {
      $action
    } catch (std::exception& e) {
      SWIG_CSharpSetPendingException(SWIG_CSharpApplicationException, e.what());
    }
  %}

  void startMission(
      const MissionSpec& mission
    , const ClientPool& client_pool
    , const MissionRecordSpec& mission_record
    , int role
    , std::string unique_experiment_id
  );

  %exception startMission(
      const MissionSpec& mission
    , const MissionRecordSpec& mission_record
  ) %{
    try {
      $action
    } catch (std::exception& e) {
      SWIG_CSharpSetPendingException(SWIG_CSharpApplicationException, e.what());
    }
  %}

  void startMission(
      const MissionSpec& mission
    , const MissionRecordSpec& mission_record
  );

  WorldState peekWorldState() const;
  
  WorldState getWorldState();

  void setVideoPolicy(AgentHost::VideoPolicy videoPolicy);

  void setRewardsPolicy(AgentHost::RewardsPolicy rewardsPolicy);

  void setObservationsPolicy(AgentHost::ObservationsPolicy observationsPolicy);

  void sendCommand(std::string command);

  std::string getRecordingTemporaryDirectory();

  void setSeed(int seed);
};
#endif

class MissionSpec {
public:
  MissionSpec();
  
  %exception MissionSpec(const std::string& rawMissionXML,bool validate) %{
    try {
      $action
    } catch (const std::runtime_error& e) {
      std::ostringstream oss;
      oss << "Caught std::runtime_error: " << e.what();
      SWIG_CSharpSetPendingException(SWIG_CSharpApplicationException, oss.str().c_str());
    }
  %}

  MissionSpec(const std::string& rawMissionXML,bool validate);

  std::string getAsXML( bool prettyPrint ) const;
  
  void setSummary( const std::string& summary );

  void timeLimitInSeconds(float s);

  void forceWorldReset();

  void setWorldSeed(const std::string& seed);

  void createDefaultTerrain();

  void setTimeOfDay(int t,bool allowTimeToPass);

  void drawBlock(int x, int y, int z, const std::string& blockType);
  
  void drawCuboid(int x1, int y1, int z1, int x2, int y2, int z2, const std::string& blockType);
  
  void drawItem(int x, int y, int z, const std::string& itemType);
  
  void drawSphere(int x, int y, int z, int radius, const std::string& blockType);
  
  void drawLine(int x1, int y1, int z1, int x2, int y2, int z2, const std::string& blockType);
  
  void startAt(float x, float y, float z);

  void startAtWithPitchAndYaw(float x, float y, float z, float pitch, float yaw);

  void endAt(float x, float y, float z, float tolerance);
  
  void setModeToCreative();
  
  void setModeToSpectator();

  void requestVideo(int width, int height);
  
  void requestVideoWithDepth(int width, int height);
  
  void setViewpoint(int viewpoint);
  
  void rewardForReachingPosition(float x, float y, float z, float amount, float tolerance);

  void observeRecentCommands();
  
  void observeHotBar();
  
  void observeFullInventory();
  
  void observeGrid(int x1,int y1,int z1,int x2,int y2,int z2, const std::string& name);
  
  void observeDistance(float x,float y,float z,const std::string& name);
  
  void observeChat();
  
  void removeAllCommandHandlers();

  void allowAllContinuousMovementCommands();
  void allowContinuousMovementCommand(const std::string& verb);

  void allowAllDiscreteMovementCommands();
  void allowDiscreteMovementCommand(const std::string& verb);

  void allowAllAbsoluteMovementCommands();
  void allowAbsoluteMovementCommand(const std::string& verb);

  void allowAllInventoryCommands();
  void allowInventoryCommand(const std::string& verb);
  
  void allowAllChatCommands();
  
  std::string getSummary() const;

  int getNumberOfAgents() const;

  bool isVideoRequested(int role) const;

  int getVideoWidth(int role) const;

  int getVideoHeight(int role) const;

  int getVideoChannels(int role) const;
  
  std::vector< std::string > getListOfCommandHandlers(int role) const;
  
  std::vector< std::string > getAllowedCommands(int role,const std::string& ) const;
};

struct TimestampedString {
private:
  TimestampedString(const TimestampedUnsignedCharVector& message);

public:
  const boost::posix_time::ptime timestamp;

  const std::string text;
};

%nodefaultctor TimestampedReward;
struct TimestampedReward {
  const boost::posix_time::ptime timestamp;

  bool hasValueOnDimension(int dimension) const;

  double getValueOnDimension(int dimension) const;

  double getValue() const;

};

struct TimestampedUnsignedCharVector {
  const boost::posix_time::ptime timestamp;

  const std::vector<unsigned char> data;
};

struct TimestampedVideoFrame {
private:
  TimestampedVideoFrame(short width, short height, short channels, TimestampedUnsignedCharVector& message);

public:
    enum FrameType {
        VIDEO
        , DEPTH_MAP
        , LUMINANCE
        , COLOUR_MAP
    };

  const boost::posix_time::ptime timestamp;

  const short width;

  const short height;

  const short channels;

  const float xPos;

  const float yPos;

  const float zPos;

  const float yaw;

  const float pitch;

  const FrameType frametype;

  const std::vector<unsigned char> pixels;
};

struct ClientInfo {
public:
    ClientInfo();
    ClientInfo(const std::string& ip_address);
    ClientInfo(const std::string& ip_address, int control_port);
    ClientInfo(const std::string& ip_address, int control_port, int command_port);

    std::string ip_address;
    int control_port;
    int command_port;
};

class ClientPool {
public:
    void add( const ClientInfo& client_info );
};

class ParameterSet{
public:
    ParameterSet();

    ParameterSet(const std::string& json);
    std::string toJson();

    void set(const std::string& key, const std::string& value);
    std::string get(const std::string& key) const;

    void setInt(const std::string& key, const int value);
    int getInt(const std::string& key) const;

    void setDouble(const std::string& key, const double value);
    double getDouble(const std::string& key) const;

    void setBool(const std::string& key, const bool value);
    bool getBool(const std::string& key) const;

    void setIterationCount(const int iteration_count);
    int getIterationCount() const;

    std::vector<std::string> keys() const;
};
