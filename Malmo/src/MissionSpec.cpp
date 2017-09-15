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
#include "FindSchemaFile.h"
#include "MissionSpec.h"
#include "Init.h"

// Boost:
#include <boost/make_shared.hpp>

// Schemas:
using namespace malmo::schemas;

// STL:
#include <algorithm>
#include <sstream>
#include <stdexcept>
using namespace std;

namespace malmo
{
    const int MillisecondsInOneSecond = 1000;

    MissionSpec::MissionSpec()
    {
        initialiser::initXSD();

        // construct a default mission
        About about("");
        FlatWorldGenerator flat_world_gen;
        flat_world_gen.generatorString( "3;7,220*1,5*3,2;3;,biome_1" );
        ServerHandlers server_handlers;
        server_handlers.FlatWorldGenerator( flat_world_gen );
        ServerQuitFromTimeUp time_up( 10000 );
        server_handlers.ServerQuitFromTimeUp().push_back( time_up );
        ServerQuitWhenAnyAgentFinishes any_quits;
        server_handlers.ServerQuitWhenAnyAgentFinishes().push_back( any_quits );
        ServerSection server_section( server_handlers );
        this->mission = boost::make_shared<Mission>( about, server_section );
        // add a single default agent
        AgentHandlers ah;
        ObservationFromFullStats obs;
        ah.ObservationFromFullStats( obs );
        ContinuousMovementCommands cmc;
        ah.ContinuousMovementCommands( cmc );
        AgentStart agent_start;
        AgentSection as( "Cristina", agent_start, ah );
        this->mission->AgentSection().push_back(as);
    }

    MissionSpec::MissionSpec(const std::string& xml, bool validate)
    {
        initialiser::initXSD();

        xml_schema::properties props;
        props.schema_location(xml_namespace, FindSchemaFile("Mission.xsd"));
        
        xml_schema::flags flags = xml_schema::flags::dont_initialize;
        if( !validate )
            flags = flags | xml_schema::flags::dont_validate;

        istringstream iss(xml);
        this->mission = Mission_(iss, flags, props);
    }

    std::string MissionSpec::getAsXML( bool prettyPrint ) const
    {
        ostringstream oss;
        
        xml_schema::namespace_infomap map;
        map[""].name = xml_namespace;
        map[""].schema = "Mission.xsd";

        xml_schema::flags flags = xml_schema::flags::dont_initialize;
        if( !prettyPrint )
            flags = flags | xml_schema::flags::dont_pretty_print;

        Mission_( oss, *this->mission, map, "UTF-8", flags );
        
        return oss.str();
    }
    
    // -------------------- settings for the server ---------------------------------
    
    void MissionSpec::setSummary( const std::string& summary )
    {
        this->mission->About().Summary( summary );
    }

    void MissionSpec::timeLimitInSeconds(float s)
    {
        ServerHandlers::ServerQuitFromTimeUp_sequence& qq = this->mission->ServerSection().ServerHandlers().ServerQuitFromTimeUp();
        if( !qq.size() ) 
        {
            // make a new ServerQuitFromTimeUp handler for the server_handlers
            ServerQuitFromTimeUp q( s * MillisecondsInOneSecond );
            qq.push_back( q );
        }
        else
        {
            ServerQuitFromTimeUp& q = qq.front();
            q.timeLimitMs( s * MillisecondsInOneSecond );
        }
    }
    
    void MissionSpec::createDefaultTerrain()
    {
        this->mission->ServerSection().ServerHandlers().FlatWorldGenerator().reset();
        DefaultWorldGenerator dwg;
        this->mission->ServerSection().ServerHandlers().DefaultWorldGenerator( dwg );
    }

    void MissionSpec::setWorldSeed(const std::string& seed)
    {
        auto& default_wg = this->mission->ServerSection().ServerHandlers().DefaultWorldGenerator();
        auto& flat_wg = this->mission->ServerSection().ServerHandlers().FlatWorldGenerator();
        if (default_wg.present())
            default_wg.get().seed(seed);
        if (flat_wg.present())
            flat_wg.get().seed(seed);
    }

    void MissionSpec::forceWorldReset()
    {
        auto& default_wg = this->mission->ServerSection().ServerHandlers().DefaultWorldGenerator();
        auto& flat_wg = this->mission->ServerSection().ServerHandlers().FlatWorldGenerator();
        auto& file_wg = this->mission->ServerSection().ServerHandlers().FileWorldGenerator();
        if (default_wg.present())
            default_wg.get().forceReset(true);
        if (flat_wg.present())
            flat_wg.get().forceReset(true);
        if (file_wg.present())
            file_wg.get().forceReset(true);
    }

    void MissionSpec::setTimeOfDay(int t,bool allowTimeToPass)
    {
        ServerSection::ServerInitialConditions_optional& sicq = this->mission->ServerSection().ServerInitialConditions();
        if( sicq.present() ) 
        {
            ServerInitialConditions sic = sicq.get();
            ServerInitialConditions::Time_optional& timeq = sic.Time();
            if( timeq.present() ) {
                Time& time = timeq.get();
                StartTime st(t);
                time.StartTime(st);
                time.AllowPassageOfTime( allowTimeToPass );
            }
            else {
                StartTime st(t);
                Time time;
                time.StartTime(st);
                time.AllowPassageOfTime(allowTimeToPass);
                timeq.set( time );
            }
        }
        else
        {
            StartTime st(t);
            Time time;
            time.StartTime(st);
            time.AllowPassageOfTime(allowTimeToPass);
            ServerInitialConditions sic;
            sic.Time(time);
            sicq.set( sic );
        }
    }

    void MissionSpec::drawBlock(int x, int y, int z, const string& blockType)
    {
        DrawBlock db( blockType, x, y, z );
        DrawingDecorator dd;
        dd.DrawObjectType().push_back( db );
        this->mission->ServerSection().ServerHandlers().DrawingDecorator().push_back( dd );
        // TODO: improve the efficiency by adding to existing elements if present?
    }

    void MissionSpec::drawCuboid(int x1, int y1, int z1, int x2, int y2, int z2, const std::string& blockType)
    {
        DrawCuboid dc(blockType, x1, y1, z1, x2, y2, z2);
        DrawingDecorator dd;
        dd.DrawObjectType().push_back(dc);
        this->mission->ServerSection().ServerHandlers().DrawingDecorator().push_back( dd );
        // TODO: improve the efficiency by adding to existing elements if present?
    }

    void MissionSpec::drawItem(int x, int y, int z, const std::string& itemType)
    {
        DrawItem di( x, y, z, itemType );
        DrawingDecorator dd;
        dd.DrawObjectType().push_back( di );
        this->mission->ServerSection().ServerHandlers().DrawingDecorator().push_back( dd );
        // TODO: improve the efficiency by adding to existing elements if present?
    }

    void MissionSpec::drawSphere(int x, int y, int z, int radius, const std::string& blockType)
    {
        DrawSphere ds(blockType, x, y, z, radius);
        DrawingDecorator dd;
        dd.DrawObjectType().push_back(ds);
        this->mission->ServerSection().ServerHandlers().DrawingDecorator().push_back( dd );
        // TODO: improve the efficiency by adding to existing elements if present?
    }

    void MissionSpec::drawLine(int x1, int y1, int z1, int x2, int y2, int z2, const std::string& blockType)
    {
        DrawLine dl( blockType, x1, y1, z1, x2, y2, z2 );
        DrawingDecorator dd;
        dd.DrawObjectType().push_back(dl);
        this->mission->ServerSection().ServerHandlers().DrawingDecorator().push_back( dd );
        // TODO: improve the efficiency by adding to existing elements if present?
    }
    
    // ------------------ settings for the agents --------------------------------

    void MissionSpec::startAt(float x, float y, float z)
    {
        this->mission->AgentSection().front().AgentStart().Placement() = PosAndDirection(x,y,z);
    }

    void MissionSpec::startAtWithPitchAndYaw(float x, float y, float z, float pitch, float yaw)
    {
        PosAndDirection pos(x, y, z);
        pos.pitch(pitch);
        pos.yaw(yaw);
        this->mission->AgentSection().front().AgentStart().Placement() = pos;
    }

    void MissionSpec::endAt(float x, float y, float z, float tolerance)
    {
        AgentHandlers::AgentQuitFromReachingPosition_optional& handler = this->mission->AgentSection().front().AgentHandlers().AgentQuitFromReachingPosition();
        PointWithToleranceAndDescription p(x, y, z);
        p.tolerance(tolerance);
        if (!handler.present())
        {
            AgentQuitFromReachingPosition quitter;
            quitter.Marker().push_back(p);
            handler.set(quitter);
        }
        else
        {
            handler->Marker().push_back(p);
        }
    }
    
    void MissionSpec::setModeToCreative()
    {
        this->mission->AgentSection().front().mode( GameMode::Creative );
    }

    void MissionSpec::setModeToSpectator()
    {
        this->mission->AgentSection().front().mode( GameMode::Spectator );
    }

    void MissionSpec::requestVideo(int width, int height)
    {
        AgentHandlers::VideoProducer_optional& vps = this->mission->AgentSection().front().AgentHandlers().VideoProducer();
        vps.set( VideoProducer( width, height ) );
    }

    void MissionSpec::requestLuminance(int width, int height)
    {
        AgentHandlers::LuminanceProducer_optional& lps = this->mission->AgentSection().front().AgentHandlers().LuminanceProducer();
        lps.set(LuminanceProducer(width, height));
    }

    void MissionSpec::requestColourMap(int width, int height)
    {
        AgentHandlers::ColourMapProducer_optional& cps = this->mission->AgentSection().front().AgentHandlers().ColourMapProducer();
        cps.set(ColourMapProducer(width, height));
    }

    void MissionSpec::request32bppDepth(int width, int height)
    {
        AgentHandlers::DepthProducer_optional& dps = this->mission->AgentSection().front().AgentHandlers().DepthProducer();
        dps.set(DepthProducer(width, height));
    }

    void MissionSpec::requestVideoWithDepth(int width, int height)
    {
        AgentHandlers::VideoProducer_optional& vps = this->mission->AgentSection().front().AgentHandlers().VideoProducer();
        VideoProducer vp(width, height);
        vp.want_depth(true);
        vps.set(vp);
    }
    
    void MissionSpec::setViewpoint(int viewpoint)
    {
        AgentHandlers::VideoProducer_optional& vps = this->mission->AgentSection().front().AgentHandlers().VideoProducer();
        if( vps.present() ) {
            vps->viewpoint(viewpoint);
        }
        // else silently do nothing since no video requested
    }

    void MissionSpec::rewardForReachingPosition(float x, float y, float z, float amount, float tolerance)
    {
        AgentHandlers::RewardForReachingPosition_optional& rrp = this->mission->AgentSection().front().AgentHandlers().RewardForReachingPosition();
        if (!rrp.present())
        {
            RewardForReachingPosition reward;
            reward.Marker().push_back(PointWithReward(x, y, z, amount, tolerance));
            rrp.set(reward);
        }
        else
        {
            rrp->Marker().push_back(PointWithReward(x, y, z, amount, tolerance));
        }
    }
    
    void MissionSpec::observeRecentCommands()
    {
        ObservationFromRecentCommands obs;
        this->mission->AgentSection().front().AgentHandlers().ObservationFromRecentCommands( obs );
    }
    
    void MissionSpec::observeHotBar()
    {
        ObservationFromHotBar obs;
        this->mission->AgentSection().front().AgentHandlers().ObservationFromHotBar( obs );
    }
    
    void MissionSpec::observeFullInventory()
    {
        ObservationFromFullInventory obs;
        this->mission->AgentSection().front().AgentHandlers().ObservationFromFullInventory( obs );
    }
    
    void MissionSpec::observeGrid(int x1,int y1,int z1,int x2,int y2,int z2,const std::string& name)
    {
        AgentHandlers::ObservationFromGrid_optional& obs = this->mission->AgentSection().front().AgentHandlers().ObservationFromGrid();
        if (!obs.present())
        {
            ObservationFromGrid observer;
            observer.Grid().push_back(GridDefinition(Pos(x1, y1, z1), Pos(x2, y2, z2), name));
            obs.set(observer);
        }
        else
        {
            obs->Grid().push_back(GridDefinition(Pos(x1, y1, z1), Pos(x2, y2, z2), name));
        }
    }
    
    void MissionSpec::observeDistance(float x, float y, float z, const std::string& name)
    {
        AgentHandlers::ObservationFromDistance_optional& obs = this->mission->AgentSection().front().AgentHandlers().ObservationFromDistance();
        if (!obs.present())
        {
            ObservationFromDistance observer;
            observer.Marker().push_back(NamedPoint(x, y, z, name));
            obs.set(observer);
        }
        else
        {
            obs->Marker().push_back(NamedPoint(x, y, z, name));
        }
    }
    
    void MissionSpec::observeChat()
    {
        AgentHandlers::ObservationFromChat_optional& obs = this->mission->AgentSection().front().AgentHandlers().ObservationFromChat();
        if (!obs.present())
        {
            ObservationFromChat observer;
            obs.set(observer);
        }
    }
    
    // ------------------ settings for the agents : command handlers --------------------------------
    
    void MissionSpec::removeAllCommandHandlers()
    {
        this->mission->AgentSection().front().AgentHandlers().ContinuousMovementCommands().reset();
        this->mission->AgentSection().front().AgentHandlers().DiscreteMovementCommands().reset();
        this->mission->AgentSection().front().AgentHandlers().AbsoluteMovementCommands().reset();
        this->mission->AgentSection().front().AgentHandlers().InventoryCommands().reset();
        this->mission->AgentSection().front().AgentHandlers().ChatCommands().reset();
        this->mission->AgentSection().front().AgentHandlers().MissionQuitCommands().reset();
    }

    void MissionSpec::allowAllContinuousMovementCommands()
    {
        ContinuousMovementCommands cmc;
        this->mission->AgentSection().front().AgentHandlers().ContinuousMovementCommands( cmc );
    }

    void MissionSpec::allowContinuousMovementCommand(const std::string& verb)
    {
        AgentHandlers::ContinuousMovementCommands_optional& cmco = this->mission->AgentSection().front().AgentHandlers().ContinuousMovementCommands();
        if( !cmco.present() )
        {
            ContinuousMovementCommands cmc;
            cmco.set( cmc );
        }
        putVerbOnList( cmco->ModifierList(), verb, "allow-list", "deny-list" );
    }

    void MissionSpec::allowAllDiscreteMovementCommands()
    {
        DiscreteMovementCommands dmc;
        this->mission->AgentSection().front().AgentHandlers().DiscreteMovementCommands( dmc );
    }

    void MissionSpec::allowDiscreteMovementCommand(const std::string& verb)
    {
        AgentHandlers::DiscreteMovementCommands_optional& dmco = this->mission->AgentSection().front().AgentHandlers().DiscreteMovementCommands();
        if( !dmco.present() )
        {
            DiscreteMovementCommands dmc;
            dmco.set( dmc );
        }
        putVerbOnList( dmco->ModifierList(), verb, "allow-list", "deny-list" );
    }

    void MissionSpec::allowAllAbsoluteMovementCommands()
    {
        AbsoluteMovementCommands amc;
        this->mission->AgentSection().front().AgentHandlers().AbsoluteMovementCommands( amc );
    }

    void MissionSpec::allowAbsoluteMovementCommand(const std::string& verb)
    {
        AgentHandlers::AbsoluteMovementCommands_optional& amco = this->mission->AgentSection().front().AgentHandlers().AbsoluteMovementCommands();
        if( !amco.present() )
        {
            AbsoluteMovementCommands amc;
            amco.set( amc );
        }
        putVerbOnList( amco->ModifierList(), verb, "allow-list", "deny-list" );
    }

    void MissionSpec::allowAllInventoryCommands()
    {
        InventoryCommands ic;
        this->mission->AgentSection().front().AgentHandlers().InventoryCommands( ic );
    }

    void MissionSpec::allowInventoryCommand(const std::string& verb)
    {
        AgentHandlers::InventoryCommands_optional& ico = this->mission->AgentSection().front().AgentHandlers().InventoryCommands();
        if( !ico.present() )
        {
            InventoryCommands ic;
            ico.set( ic );
        }
        putVerbOnList( ico->ModifierList(), verb, "allow-list", "deny-list" );
    }
   
    void MissionSpec::allowAllChatCommands()
    {
        ChatCommands cc;
        this->mission->AgentSection().front().AgentHandlers().ChatCommands( cc );
    }

    // ------------------------------- information ---------------------------------------------------
    
    string MissionSpec::getSummary() const
    {
        return this->mission->About().Summary();
    }
    
    int MissionSpec::getNumberOfAgents() const
    {
        return (int)this->mission->AgentSection().size();
    }
    
    bool MissionSpec::isVideoRequested(int role) const
    {
        return this->mission->AgentSection()[role].AgentHandlers().VideoProducer().present();
    }
    
    bool MissionSpec::isDepthRequested(int role) const
    {
        return this->mission->AgentSection()[role].AgentHandlers().DepthProducer().present();
    }

    bool MissionSpec::isLuminanceRequested(int role) const
    {
        return this->mission->AgentSection()[role].AgentHandlers().LuminanceProducer().present();
    }

    bool MissionSpec::isColourMapRequested(int role) const
    {
        return this->mission->AgentSection()[role].AgentHandlers().ColourMapProducer().present();
    }

    int MissionSpec::getVideoWidth(int role) const
    {
        if (!isVideoRequested(role) && !isDepthRequested(role) && !isLuminanceRequested(role) && !isColourMapRequested(role))
            throw runtime_error("MissionInitSpec::getVideoWidth : video has not been requested for this role");

        AgentHandlers::VideoProducer_optional& vps = this->mission->AgentSection()[role].AgentHandlers().VideoProducer();
        AgentHandlers::DepthProducer_optional& dps = this->mission->AgentSection()[role].AgentHandlers().DepthProducer();
        AgentHandlers::LuminanceProducer_optional& lps = this->mission->AgentSection()[role].AgentHandlers().LuminanceProducer();
        AgentHandlers::ColourMapProducer_optional& cps = this->mission->AgentSection()[role].AgentHandlers().ColourMapProducer();
        return vps.present() ? vps->Width() : (dps.present() ? dps->Width() : (lps.present() ? lps->Width() : cps->Width()));
    }

    int MissionSpec::getVideoHeight(int role) const
    {
        if (!isVideoRequested(role) && !isDepthRequested(role) && !isLuminanceRequested(role) && !isColourMapRequested(role))
            throw runtime_error("MissionInitSpec::getVideoHeight : video has not been requested for this role");

        AgentHandlers::VideoProducer_optional& vps = this->mission->AgentSection()[role].AgentHandlers().VideoProducer();
        AgentHandlers::DepthProducer_optional& dps = this->mission->AgentSection()[role].AgentHandlers().DepthProducer();
        AgentHandlers::LuminanceProducer_optional& lps = this->mission->AgentSection()[role].AgentHandlers().LuminanceProducer();
        AgentHandlers::ColourMapProducer_optional& cps = this->mission->AgentSection()[role].AgentHandlers().ColourMapProducer();
        return vps.present() ? vps->Height() : (dps.present() ? dps->Height() : (lps.present() ? lps->Height() : cps->Height()));
    }

    int MissionSpec::getVideoChannels(int role) const
    {
        // Only deals with video producer; depth producer always returns 32bpp; luminance producer always returns 8bpp; colourmap producer always returns 24bpp.
        if (!isVideoRequested(role))
            throw runtime_error("MissionInitSpec::getVideoChannels : video has not been requested for this role");

        AgentHandlers::VideoProducer_optional& vps = this->mission->AgentSection()[role].AgentHandlers().VideoProducer();
        return vps->want_depth() ? 4 : 3;
    }

    vector<string> MissionSpec::getListOfCommandHandlers(int role) const
    {
        vector<string> command_handlers;
        AgentHandlers ah = this->mission->AgentSection()[role].AgentHandlers();
        if( ah.ContinuousMovementCommands().present() )
            command_handlers.push_back( "ContinuousMovement" );
        if( ah.AbsoluteMovementCommands().present() )
            command_handlers.push_back( "AbsoluteMovement" );
        if( ah.DiscreteMovementCommands().present() )
            command_handlers.push_back( "DiscreteMovement" );
        if( ah.InventoryCommands().present() )
            command_handlers.push_back( "Inventory" );
        if( ah.ChatCommands().present() )
            command_handlers.push_back( "Chat" );
        if( ah.SimpleCraftCommands().present() )
            command_handlers.push_back( "SimpleCraft" );
        if( ah.MissionQuitCommands().present() )
            command_handlers.push_back( "MissionQuit" );
        return command_handlers;
    }
    
    vector<string> MissionSpec::getAllowedCommands(int role,const string& command_handler) const
    {
        AgentHandlers ah = this->mission->AgentSection()[role].AgentHandlers();
        if( command_handler == "ContinuousMovement" && ah.ContinuousMovementCommands().present() ) {
            vector<string> commands( begin(ContinuousMovementCommand::_xsd_ContinuousMovementCommand_literals_), end(ContinuousMovementCommand::_xsd_ContinuousMovementCommand_literals_) );
            if( ah.ContinuousMovementCommands()->ModifierList().present() )
                return getModifiedCommandList( commands, *ah.ContinuousMovementCommands()->ModifierList() );
            else
                return commands;
        }
        else if( command_handler == "AbsoluteMovement" && ah.AbsoluteMovementCommands().present() ) {
            vector<string> commands( begin(AbsoluteMovementCommand::_xsd_AbsoluteMovementCommand_literals_), end(AbsoluteMovementCommand::_xsd_AbsoluteMovementCommand_literals_) );
            if( ah.AbsoluteMovementCommands()->ModifierList().present() )
                return getModifiedCommandList( commands, *ah.AbsoluteMovementCommands()->ModifierList() );
            else
                return commands;
        }
        else if( command_handler == "DiscreteMovement" && ah.DiscreteMovementCommands().present() ) {
            vector<string> commands( begin(DiscreteMovementCommand::_xsd_DiscreteMovementCommand_literals_), end(DiscreteMovementCommand::_xsd_DiscreteMovementCommand_literals_) );
            if( ah.DiscreteMovementCommands()->ModifierList().present() )
                return getModifiedCommandList( commands, *ah.DiscreteMovementCommands()->ModifierList() );
            else
                return commands;
        }
        else if( command_handler == "Inventory" && ah.InventoryCommands().present() ) {
            vector<string> commands( begin(InventoryCommand::_xsd_InventoryCommand_literals_), end(InventoryCommand::_xsd_InventoryCommand_literals_) );
            if( ah.InventoryCommands()->ModifierList().present() )
                return getModifiedCommandList( commands, *ah.InventoryCommands()->ModifierList() );
            else
                return commands;
        }
        else if( command_handler == "Chat" && ah.ChatCommands().present() ) {
            vector<string> commands( begin(ChatCommand::_xsd_ChatCommand_literals_), end(ChatCommand::_xsd_ChatCommand_literals_) );
            if( ah.ChatCommands()->ModifierList().present() )
                return getModifiedCommandList( commands, *ah.ChatCommands()->ModifierList() );
            else
                return commands;
        }
        else if( command_handler == "SimpleCraft" && ah.SimpleCraftCommands().present() ) {
            vector<string> commands( begin(SimpleCraftCommand::_xsd_SimpleCraftCommand_literals_), end(SimpleCraftCommand::_xsd_SimpleCraftCommand_literals_) );
            if( ah.SimpleCraftCommands()->ModifierList().present() )
                return getModifiedCommandList( commands, *ah.SimpleCraftCommands()->ModifierList() );
            else
                return commands;
        }
        else if( command_handler == "MissionQuit" && ah.MissionQuitCommands().present() ) {
            vector<string> commands( begin(MissionQuitCommand::_xsd_MissionQuitCommand_literals_), end(MissionQuitCommand::_xsd_MissionQuitCommand_literals_) );
            if( ah.MissionQuitCommands()->ModifierList().present() )
                return getModifiedCommandList( commands, *ah.MissionQuitCommands()->ModifierList() );
            else
                return commands;
        }
        throw runtime_error( "Unexpected command handler name: " + command_handler );
    }
    
    // ---------------------------- private functions -----------------------------------------------
    
    void MissionSpec::putVerbOnList( ::xsd::cxx::tree::optional< ModifierList >& mlo
                                   , const std::string& verb
                                   , const std::string& on_list
                                   , const std::string& off_list )
    {
        // remove the off-list if one present
        if( mlo.present() && mlo->type() == off_list )
        {
            mlo.reset();
        }
        // add an on-list if not present
        if( !mlo.present() )
        {
            ModifierList allow_list;
            allow_list.type( on_list );
            mlo.set( allow_list );
        }
        // add the command verb to the modifier list, if not already there
        ModifierList::command_sequence& cs = mlo->command();
        if( find( cs.begin(), cs.end(), verb ) == cs.end() )
        {
            cs.push_back( verb );
        }
        // (else silent continue - no need to alarm the user if the command is already there)
    }

    vector<string> MissionSpec::getModifiedCommandList( const vector<string>& all_commands, const CommandListModifier& modifier_list )
    {
        vector<string> listed_commands( modifier_list.command().begin(), modifier_list.command().end() );
        switch( modifier_list.type() ) {
            case type::allow_list:
                return listed_commands;
            case type::deny_list:
                vector<string> full_commands( all_commands );
                sort( full_commands.begin(), full_commands.end() );
                sort( listed_commands.begin(), listed_commands.end() );
                vector<string> remaining_commands;
                std::set_difference(
                    full_commands.begin(), full_commands.end(),
                    listed_commands.begin(), listed_commands.end(),
                    std::back_inserter( remaining_commands )
                );
                return remaining_commands;
        }
        throw runtime_error( "Unexpected modifier list type." );
    }

    std::ostream& operator<<(std::ostream& os, const MissionSpec& ms)
    {
        os << "MissionSpec:\n";
        os << ms.getAsXML(true);
        return os;
    }
}
