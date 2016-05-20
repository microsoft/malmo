// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

// Local:
#include "MissionSpec.h"

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
        xml_schema::properties props;
        props.schema_location(xml_namespace, "Mission.xsd");
        
        xml_schema::flags flags = 0;
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

        xml_schema::flags flags = 0;
        if( !prettyPrint )
            flags = flags | xml_schema::flags::dont_pretty_print;

        Mission_( oss, *this->mission, map, "UTF-8", flags );
        
        return oss.str();
    }
    
    // -------------------- settings for the server ---------------------------------

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
                Time time(st);
                time.AllowPassageOfTime(allowTimeToPass);
                timeq.set( time );
            }
        }
        else
        {
            StartTime st(t);
            Time time(st);
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
    
    void MissionSpec::startAt(int x, int y, int z)
    {
        this->mission->AgentSection().front().AgentStart().Placement() = PosAndDirection(x,y,z);
    }

    void MissionSpec::endAt(int x, int y, int z)
    {
        AgentHandlers::AgentQuitFromReachingPosition_optional& handler = this->mission->AgentSection().front().AgentHandlers().AgentQuitFromReachingPosition();
        if (!handler.present())
        {
            AgentQuitFromReachingPosition quitter;
            quitter.Marker().push_back(PointWithToleranceAndDescription(x, y, z));
            handler.set(quitter);
        }
        else
        {
            handler->Marker().push_back(PointWithToleranceAndDescription(x, y, z));
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
        if( vps.present() )
            throw runtime_error( "MissionSpec::requestVideo : video was already requested for this agent" );
        vps.set( VideoProducer( width, height ) );
    }
    
    void MissionSpec::rewardForReachingPosition(int x, int y, int z, float amount, float tolerance)
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
    
    void MissionSpec::observeDistance(int x, int y, int z, const std::string& name)
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
    
    int MissionSpec::getNumberOfAgents() const
    {
        return (int)this->mission->AgentSection().size();
    }
    
    bool MissionSpec::isVideoRequested(int role) const
    {
        return this->mission->AgentSection()[role].AgentHandlers().VideoProducer().present();
    }
    
    int MissionSpec::getVideoWidth(int role) const
    {
        AgentHandlers::VideoProducer_optional& vps = this->mission->AgentSection()[role].AgentHandlers().VideoProducer();
        if( !vps.present() )
            throw runtime_error("MissionInitSpec::getVideoWidth : video has not been requested for this role");
        return vps->Width();
    }
    
    int MissionSpec::getVideoHeight(int role) const
    {
        AgentHandlers::VideoProducer_optional& vps = this->mission->AgentSection()[role].AgentHandlers().VideoProducer();
        if( !vps.present() )
            throw runtime_error("MissionInitSpec::getVideoHeight : video has not been requested for this role");
        return vps->Height();
    }
    
    int MissionSpec::getVideoChannels(int role) const
    {
        AgentHandlers::VideoProducer_optional& vps = this->mission->AgentSection()[role].AgentHandlers().VideoProducer();
        if( !vps.present() )
            throw runtime_error("MissionInitSpec::getVideoChannels : video has not been requested for this role");
        return vps->want_depth() ? 4 : 3;
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
}
