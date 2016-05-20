// --------------------------------------------------------------------------------------------------------------------
// <copyright file="App.xaml.cs" company="Microsoft Corporation">
//   Copyright (C) Microsoft Corporation.  All rights reserved.
// </copyright>
// --------------------------------------------------------------------------------------------------------------------
namespace Microsoft.Research.Malmo.HumanAction
{
    using System.Windows;
    using System.Windows.Threading;

    /// <summary>
    /// Interaction logic for App XAML.
    /// </summary>
    public partial class App
    {
        /// <summary>
        /// Catch an exception in the application.
        /// </summary>
        /// <param name="sender">This parameter is not used.</param>
        /// <param name="e">The exception's arguments.</param>
        public void ApplicationDispatcherUnhandledException(object sender, DispatcherUnhandledExceptionEventArgs e)
        {
            // Process unhandled exception
            MessageBox.Show("An unhandled exception just occurred: " + e.Exception.Message, "Exception Sample", MessageBoxButton.OK, MessageBoxImage.Warning);

            // Prevent default unhandled exception processing
            e.Handled = true;
        }
    }
}
