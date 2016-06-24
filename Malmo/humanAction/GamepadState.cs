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

namespace Microsoft.Research.Malmo.HumanAction
{
    using System;
    using SlimDX;
    using SlimDX.XInput;

    /// <summary>
    /// Wrapper around SlimDX Gamepad state and XInput.
    /// </summary>
    public class GamepadState
    {
        /// <summary>
        /// The user index of this gamepad
        /// </summary>
        public readonly UserIndex UserIndex;

        /// <summary>
        /// The underlying XInput controller object for this gamepad
        /// </summary>
        public readonly Controller Controller;

        /// <summary>
        /// The number of the most recent packet.
        /// </summary>
        private uint lastPacket;

        /// <summary>
        /// Initializes a new instance of the <see cref="GamepadState"/> class.
        /// </summary>
        /// <param name="userIndex">The index of the user whose gamepad this object should model</param>
        public GamepadState(UserIndex userIndex)
        {
            this.UserIndex = userIndex;
            this.Controller = new Controller(userIndex);
        }

        /// <summary>
        /// Gets the state of the DPad.
        /// </summary>
        public DPadState DPad { get; private set; }

        /// <summary>
        /// Gets the state of the left stick.
        /// </summary>
        public ThumbstickState LeftStick { get; private set; }

        /// <summary>
        /// Gets the state of the right stick.
        /// </summary>
        public ThumbstickState RightStick { get; private set; }

        /// <summary>
        /// Gets a value indicating whether the A button is pressed.
        /// </summary>
        public bool A { get; private set; }

        /// <summary>
        /// Gets a value indicating whether the B button is pressed.
        /// </summary>
        public bool B { get; private set; }

        /// <summary>
        /// Gets a value indicating whether the X button is pressed.
        /// </summary>
        public bool X { get; private set; }

        /// <summary>
        /// Gets a value indicating whether the Y button is pressed.
        /// </summary>
        public bool Y { get; private set; }

        /// <summary>
        /// Gets a value indicating whether the right shoulder button is pressed.
        /// </summary>
        public bool RightShoulder { get; private set; }

        /// <summary>
        /// Gets a value indicating whether the left shoulder button is pressed.
        /// </summary>
        public bool LeftShoulder { get; private set; }

        /// <summary>
        /// Gets a value indicating whether the start button is pressed.
        /// </summary>
        public bool Start { get; private set; }

        /// <summary>
        /// Gets a value indicating whether the back button is pressed.
        /// </summary>
        public bool Back { get; private set; }

        /// <summary>
        /// Gets a value indicating whether the right trigger is pulled.
        /// </summary>
        public bool RightTrigger { get; private set; }

        /// <summary>
        /// Gets a value indicating whether the left trigger is pulled.
        /// </summary>
        public bool LeftTrigger { get; private set; }

        /// <summary>
        /// Gets a value indicating whether the desired gamepad is connected.
        /// </summary>
        public bool IsConnected
        {
            // If get a crash here then maybe need to install the SlimDX Developer SDK:
            // http://slimdx.org/download.php
            get { return this.Controller.IsConnected; } 
        }

        /// <summary>
        /// Vibrates the gamepad.
        /// </summary>
        /// <param name="leftMotor">The amount to vibrate the left motor</param>
        /// <param name="rightMotor">The amount to vibrate the right motor</param>
        public void Vibrate(float leftMotor, float rightMotor)
        {
            this.Controller.SetVibration(new Vibration
            {
                LeftMotorSpeed = (ushort)(this.Saturate(leftMotor) * ushort.MaxValue),
                RightMotorSpeed = (ushort)(this.Saturate(rightMotor) * ushort.MaxValue)
            });
        }

        /// <summary>
        /// Updates the state of the gamepad.
        /// </summary>
        public void Update()
        {
            // If not connected, nothing to update
            if (!this.IsConnected)
            {
                return;
            }

            State state = this.Controller.GetState();

            if (this.lastPacket == state.PacketNumber)
            {
                return;
            }

            this.lastPacket = state.PacketNumber;

            var gamepadState = state.Gamepad;

            // Shoulders
            this.LeftShoulder = (gamepadState.Buttons & GamepadButtonFlags.LeftShoulder) != 0;
            this.RightShoulder = (gamepadState.Buttons & GamepadButtonFlags.RightShoulder) != 0;

            // Triggers
            this.LeftTrigger = gamepadState.LeftTrigger > Gamepad.GamepadTriggerThreshold;
            this.RightTrigger = gamepadState.RightTrigger > Gamepad.GamepadTriggerThreshold;

            // Buttons
            this.Start = (gamepadState.Buttons & GamepadButtonFlags.Start) != 0;
            this.Back = (gamepadState.Buttons & GamepadButtonFlags.Back) != 0;

            this.A = (gamepadState.Buttons & GamepadButtonFlags.A) != 0;
            this.B = (gamepadState.Buttons & GamepadButtonFlags.B) != 0;
            this.X = (gamepadState.Buttons & GamepadButtonFlags.X) != 0;
            this.Y = (gamepadState.Buttons & GamepadButtonFlags.Y) != 0;

            // D-Pad
            this.DPad = new DPadState(
                                 (gamepadState.Buttons & GamepadButtonFlags.DPadUp) != 0,
                                 (gamepadState.Buttons & GamepadButtonFlags.DPadDown) != 0,
                                 (gamepadState.Buttons & GamepadButtonFlags.DPadLeft) != 0,
                                 (gamepadState.Buttons & GamepadButtonFlags.DPadRight) != 0);

            // Thumbsticks
            this.LeftStick = new ThumbstickState(
                this.Normalize(gamepadState.LeftThumbX, gamepadState.LeftThumbY, Gamepad.GamepadLeftThumbDeadZone),
                (gamepadState.Buttons & GamepadButtonFlags.LeftThumb) != 0);
            this.RightStick = new ThumbstickState(
                this.Normalize(gamepadState.RightThumbX, gamepadState.RightThumbY, Gamepad.GamepadRightThumbDeadZone),
                (gamepadState.Buttons & GamepadButtonFlags.RightThumb) != 0);
        }

        /// <summary>
        /// Normalizes the input, providing dead zone handling.
        /// </summary>
        /// <param name="rawX">The raw X value</param>
        /// <param name="rawY">The raw Y value</param>
        /// <param name="threshold">The dead zone threshold</param>
        /// <returns>The normalized vector</returns>
        private Vector2 Normalize(short rawX, short rawY, short threshold)
        {
            var value = new Vector2(rawX, rawY);
            var magnitude = value.Length();
            var direction = value / (magnitude == 0 ? 1 : magnitude);

            var normalizedMagnitude = 0.0f;
            if (magnitude - threshold > 0)
            {
                normalizedMagnitude = Math.Min((magnitude - threshold) / (short.MaxValue - threshold), 1);
            }

            return direction * normalizedMagnitude;
        }

        /// <summary>
        /// Bounds the value to be within 0 and 1.
        /// </summary>
        /// <param name="value">The value</param>
        /// <returns>A value between 0 and 1</returns>
        private float Saturate(float value)
        {
            return value < 0 ? 0 : value > 1 ? 1 : value;
        }

        /// <summary>
        /// Value type representing the state of the Directional Pad.
        /// </summary>
        public struct DPadState
        {
            /// <summary>
            /// Value indicating whether the Up direction is pressed.
            /// </summary>
            public readonly bool Up;

            /// <summary>
            /// Value indicating whether the Down direction is pressed.
            /// </summary>
            public readonly bool Down;

            /// <summary>
            /// Value indicating whether the Left direction is pressed.
            /// </summary>
            public readonly bool Left;

            /// <summary>
            /// Value indicating whether the Right direction is pressed.
            /// </summary>
            public readonly bool Right;

            /// <summary>
            /// Initializes a new instance of the <see cref="DPadState"/> struct.
            /// </summary>
            /// <param name="up">Whether the up button is pressed</param>
            /// <param name="down">Whether the down button is pressed</param>
            /// <param name="left">Whether the left button is pressed</param>
            /// <param name="right">Whether the right button is pressed</param>
            public DPadState(bool up, bool down, bool left, bool right)
            {
                this.Up = up;
                this.Down = down;
                this.Left = left;
                this.Right = right;
            }
        }

        /// <summary>
        /// Value type representing the state of a thumbstick.
        /// </summary>
        public struct ThumbstickState
        {
            /// <summary>
            /// The position of the thumbstick.
            /// </summary>
            public readonly Vector2 Position;

            /// <summary>
            /// Value indicating whether the thumbstick has been clicked.
            /// </summary>
            public readonly bool Clicked;

            /// <summary>
            /// Initializes a new instance of the <see cref="ThumbstickState"/> struct.
            /// </summary>
            /// <param name="position">The position of the thumbstick</param>
            /// <param name="clicked">Whether the thumbstick has been clicked</param>
            public ThumbstickState(Vector2 position, bool clicked)
            {
                this.Clicked = clicked;
                this.Position = position;
            }
        }
    }
}
