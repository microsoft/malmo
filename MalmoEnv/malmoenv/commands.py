# ------------------------------------------------------------------------------------------------
# Copyright (c) 2018 Microsoft Corporation
#
# Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
# associated documentation files (the "Software"), to deal in the Software without restriction,
# including without limitation the rights to use, copy, modify, merge, publish, distribute,
# sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all copies or
# substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
# NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
# NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
# DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
# ------------------------------------------------------------------------------------------------

from lxml import etree
import argparse


class CommandHandlerException(Exception):
    """For reporting errors in parsing Agent Command Handlers"""
    def __init__(self, message):
        self.message = message


class CommandParser:
    """Parse Agent Command Handlers to a list"""
    continuousMovementCommands = "ContinuousMovement"
    absoluteMovementCommands = "AbsoluteMovement"
    discreteMovementCommands = "DiscreteMovement"
    inventoryCommands = "Inventory"
    chatCommands = "Chat"
    simpleCraftCommands = "SimpleCraft"
    nearbyCraftCommands = "NearbyCraft"
    nearbySmeltCommands = "NearbySmelt"
    missionQuitCommands = "MissionQuit"
    humanLevelCommands = "HumanLevel"

    ns = "{http://ProjectMalmo.microsoft.com}"

    all_continuous = ["jump", "move", "pitch", "strafe", "turn", "crouch", "attack", "use"]
    all_absolute = ["tpx", "tpy", "tpz", "tp", "setYaw", "setPitch"]
    all_discrete = ["move", "jumpmove", "strafe", "jumpstrafe", "turn", "movenorth", "moveeast",
                    "movesouth", "movewest", "jumpnorth", "jumpeast", "jumpsouth", "jumpwest",
                    "jump", "look", "attack", "use", "jumpuse"]
    all_inventory = ["swapInventoryItems", "combineInventoryItems", "discardCurrentItem",
                     "hotbar.1", "hotbar.2", "hotbar.3", "hotbar.4", "hotbar.5", "hotbar.6",
                     "hotbar.7", "hotbar.8", "hotbar.9"]
    all_chat = ["chat"]
    all_simplecraft = ["craft"]
    all_nearbycraft = ["nearbyCraft"]
    all_nearbysmelt = ["nearbySmelt"]
    all_mission_quit = ["quit"]
    all_human_level = ["forward", "left", "right", "jump", "sneak", "sprint", "inventory",
                       "swapHands", "drop", "use", "attack", "moveMouse",
                       "hotbar.1", "hotbar.2", "hotbar.3", "hotbar.4", "hotbar.5",
                       "hotbar.6", "hotbar.7", "hotbar.8", "hotbar.9"]

    def __init__(self, action_filter=None):
        if action_filter is None:
            action_filter = []
        self.action_filter = action_filter
        pass

    def get_commands(self, mission_xml, role):
        """Get commands from xml string as a list of (command_type:int, turnbased:boolean, command:string)"""
        mission = etree.fromstring(mission_xml)
        return self.get_commands_from_xml(mission, role)

    def get_commands_from_file(self, mission_file, role):
        """Get commands from xml file as a list of (command_type:int, turnbased:boolean, command:string)"""
        doc = etree.parse(mission_file)
        mission = doc.getroot()
        return self.get_commands_from_xml(mission, role)

    def get_commands_from_xml(self, mission, role):
        """Get commands from etree"""
        handlers = mission.findall(CommandParser.ns + "AgentSection" + "/" + CommandParser.ns + "AgentHandlers")
        if len(handlers) <= role:
            raise CommandHandlerException("Not enough agents sections in XML")
        commands = []
        self._command_hander(handlers[role], False, commands)
        return commands

    def get_actions(self, commands):
        """Get parameterized actions from command list based on command type and verb."""
        actions = []
        for type, turn_based, verb in commands:
            if len(self.action_filter) != 0 and verb not in self.action_filter:
                continue
            if type == 'DiscreteMovement':
                if verb in {"move", "turn", "look",
                            "strafe", "jumpmove", "jumpstrafe"}:
                    actions.append(verb + " 1")
                    actions.append(verb + " -1")
                elif verb in {"jumpeast", "jumpnorth", "jumpsouth",
                              "jumpwest", "movenorth", "moveeast",
                              "movesouth", "movewest", "jumpuse",
                              "use", "attack", "jump"}:
                    actions.append(verb + " 1")
                else:
                    raise CommandHandlerException("Invalid discrete command")
            elif type == 'ContinuousMovement':
                #  Translate to discrete.
                if verb in {"move", "strafe", "pitch", "turn"}:
                    actions.append(verb + " 1")
                    actions.append(verb + " -1")
                elif verb in {"crouch", "jump", "attack", "use"}:
                    actions.append(verb + " 1")
                    actions.append(verb + " 0")
                else:
                    raise CommandHandlerException("Invalid continuous command")
            elif type == 'HumanLevel':
                if verb == 'moveMouse':
                    actions.append('mouseMove 0 0')
                elif verb in {'forward', 'back', 'left', 'right'}:
                    actions.append(verb + ' 1')
                    actions.append(verb + ' 0')
                else:
                    actions.append(verb)
            elif type == 'MissionQuit':
                if verb != 'quit':
                    raise CommandHandlerException("Invalid quit command")
                actions.append(verb)
            elif type == 'Chat':
                if verb != 'chat':
                    raise CommandHandlerException("Invalid chat command")
                actions.append(verb)
            elif type == 'NearbyCraft':
                if verb != 'nearbyCraft':
                    raise CommandHandlerException("Invalid nearby craft command")
                actions.append(verb)
            elif type == 'NearbySmelt':
                if verb != 'nearbySmelt':
                    raise CommandHandlerException("Invalid nearby smelt command")
                actions.append(verb)
            elif type == 'SimpleCraft':
                if verb != 'craft':
                    raise CommandHandlerException("Invalid craft command")
                actions.append(verb)
            elif type == 'AbsoluteMovement' or 'Inventory':
                actions.append(verb)
        return actions

    def _command_hander(self, handlers, turnbased, commands):
        for ch in handlers:
            if ch.tag == CommandParser.ns + "TurnBasedCommands":
                self._command_hander(ch, True, commands)
            elif ch.tag == CommandParser.ns + "ContinuousMovementCommands":
                if turnbased:
                    raise CommandHandlerException("ContinuouMovementCommands not allowed in TurnBased")
                commands.extend(self._add_commands(CommandParser.continuousMovementCommands, turnbased, ch))
            elif ch.tag == CommandParser.ns + "AbsoluteMovementCommands":
                commands.extend(self._add_commands(CommandParser.absoluteMovementCommands, turnbased, ch))
            elif ch.tag == CommandParser.ns + "DiscreteMovementCommands":
                commands.extend(self._add_commands(CommandParser.discreteMovementCommands, turnbased, ch))
            elif ch.tag == CommandParser.ns + "InventoryCommands":
                commands.extend(self._add_commands(CommandParser.inventoryCommands, turnbased, ch))
            elif ch.tag == CommandParser.ns + "ChatCommands":
                commands.extend(self._add_commands(CommandParser.chatCommands, turnbased, ch))
            elif ch.tag == CommandParser.ns + "SimpleCraftCommands":
                commands.extend(self._add_commands(CommandParser.simpleCraftCommands, turnbased, ch))
            elif ch.tag == CommandParser.ns + "NearbyCraftCommands":
                commands.extend(self._add_commands(CommandParser.nearbyCraftCommands, turnbased, ch))
            elif ch.tag == CommandParser.ns + "NearbySmeltCommands":
                commands.extend(self._add_commands(CommandParser.nearbySmeltCommands, turnbased, ch))
            elif ch.tag == CommandParser.ns + "MissionQuitCommands":
                commands.extend(self._add_commands(CommandParser.missionQuitCommands, turnbased, ch))
            elif ch.tag == CommandParser.ns + "HumanLevelCommands":
                if turnbased:
                    raise CommandHandlerException("HumanLevelCommands not allowed in TurnBased")
                commands.extend(self._add_commands(CommandParser.humanLevelCommands, turnbased, ch))

    def _add_commands(self, command_type, turnbased, elem):
        deny = []
        allow = []
        for ml in elem:
            if ml.tag == CommandParser.ns + "ModifierList":
                # print("**ModifierList**", ml.attrib['type'])
                for cmd in ml:
                    if cmd.tag == CommandParser.ns + "command":
                        # print("Command", cmd.text)
                        if ml.attrib['type'] == 'deny-list':
                            deny.append((command_type, turnbased, cmd.text))
                        if ml.attrib['type'] == 'allow-list':
                            allow.append((command_type, turnbased, cmd.text))
        # print("allow", allow)
        # print("deny", deny)
        return self._fill_command_list(command_type, turnbased, allow, deny)

    def _fill_command_list(self, command_type, turnbased, allow, deny):
        # Add allow defaults for empty allow list.
        if not allow:
            if command_type == CommandParser.discreteMovementCommands:
                allow = [(command_type, turnbased, c) for c in CommandParser.all_discrete]
            elif command_type == CommandParser.continuousMovementCommands:
                allow = [(command_type, turnbased, c) for c in CommandParser.all_continuous]
            elif command_type == CommandParser.absoluteMovementCommands:
                allow = [(command_type, turnbased, c) for c in CommandParser.all_absolute]
            elif command_type == CommandParser.humanLevelCommands:
                allow = [(command_type, turnbased, c) for c in CommandParser.all_human_level]
            elif command_type == CommandParser.simpleCraftCommands:
                allow = [(command_type, turnbased, c) for c in CommandParser.all_simplecraft]
            elif command_type == CommandParser.nearbyCraftCommands:
                allow = [(command_type, turnbased, c) for c in CommandParser.all_nearbycraft]
            elif command_type == CommandParser.nearbySmeltCommands:
                allow = [(command_type, turnbased, c) for c in CommandParser.all_nearbysmelt]
            elif command_type == CommandParser.missionQuitCommands:
                allow = [(command_type, turnbased, c) for c in CommandParser.missionQuitCommands]
            elif command_type == CommandParser.chatCommands:
                allow = [(command_type, turnbased, c) for c in CommandParser.all_chat]
            elif command_type == CommandParser.inventoryCommands:
                allow = [(command_type, turnbased, c) for c in CommandParser.all_inventory]

        # Remove all denied.
        for d in deny:
            while d in allow:
                allow.remove(d)
        # Restrict to (optional) competition commands.
        if len(self.action_filter) > 0:
            filtered = [(command_type, turnbased, c) for c in self.action_filter]
            return [a for a in allow if a in filtered]
        else:
            return allow


def main():
    xml_file = "mission.xml"
    parser = argparse.ArgumentParser(description='Command handler xml parsing test')
    parser.add_argument('--mission_file', type=str, default=xml_file, help='the mission xml')
    parser.add_argument('--role', type=int, default=0, help='the agent role')
    args = parser.parse_args()

    xml_file = args.mission_file
    role = args.role

    print(xml_file)
    action_filter = ["turn", "move", "use", "attack"]
    c = CommandParser(action_filter)
    command_list = c.get_commands_from_file(xml_file, role)
    print(command_list)


if __name__ == "__main__":
    main()
