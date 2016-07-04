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

class MissionRecordSpec
{
public:
    MissionRecordSpec();
    MissionRecordSpec(std::string destination);
    void recordMP4(int frames_per_second, int64_t bit_rate);
    void recordObservations();
    void recordRewards();
    void recordCommands();        
    std::string getTemporaryDirectory();
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

  void setVideoPolicy(VideoPolicy videoPolicy);

  void setRewardsPolicy(RewardsPolicy rewardsPolicy);

  void setObservationsPolicy(ObservationsPolicy observationsPolicy);

  void sendCommand(std::string command);
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
};
#endif

class MissionSpec {
public:
  MissionSpec();
  
  %exception MissionSpec(const std::string& rawMissionXML,bool validate) %{
    try {
      $action
    } catch (const xml_schema::exception& e) {
      std::ostringstream oss;
      oss << "Caught xml_schema::exception: " << e.what() << "\n" << e;
      SWIG_CSharpSetPendingException(SWIG_CSharpApplicationException, oss.str().c_str());
    }
  %}

  MissionSpec(const std::string& rawMissionXML,bool validate);

  std::string getAsXML( bool prettyPrint ) const;
  
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
  
  int getNumberOfAgents() const;

  bool isVideoRequested(int role) const;

  int getVideoWidth(int role) const;

  int getVideoHeight(int role) const;

  int getVideoChannels(int role) const;
};

struct TimestampedString {
private:
  TimestampedString(const TimestampedUnsignedCharVector& message);

public:
  const boost::posix_time::ptime timestamp;

  const std::string text;
};

struct TimestampedReward {
  const boost::posix_time::ptime timestamp;

  bool hasValueOnDimension(int dimension) const;

  float getValueOnDimension(int dimension) const;

  float getValue() const;

};

struct TimestampedUnsignedCharVector {
  const boost::posix_time::ptime timestamp;

  const std::vector<unsigned char> data;
};

struct TimestampedVideoFrame {
private:
  TimestampedVideoFrame(short width, short height, short channels, TimestampedUnsignedCharVector& message);

public:
  const boost::posix_time::ptime timestamp;

  const short width;

  const short height;

  const short channels;

  const std::vector<unsigned char> pixels;
};

struct ClientInfo {
public:
    ClientInfo();
    ClientInfo(const std::string& ip_address);
    ClientInfo(const std::string& ip_address, int port);

    std::string ip_address;
    int port;
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
