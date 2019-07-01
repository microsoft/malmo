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

package com.microsoft.Malmo.Utils;

import net.minecraftforge.common.config.Configuration;

import com.microsoft.Malmo.MalmoMod;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.util.List;
import java.util.Date;
import java.util.Iterator;
import java.util.logging.*;
import java.util.logging.Level;

import org.apache.commons.lang3.time.DateFormatUtils;
import javax.xml.bind.JAXBException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.microsoft.Malmo.Schemas.AgentSection;
import com.microsoft.Malmo.Schemas.Mission;
import com.microsoft.Malmo.Schemas.MissionInit;
import com.microsoft.Malmo.Schemas.Reward;

/** Class that helps to centralise optional logging of mission rewards.<br>
 */
public class PerformanceHelper
{
    private static String outDir;
    private static boolean performanceEnabled = false;

    /** Initialize scoing. */
    static public void update(Configuration configs)
    {

        outDir = configs.get(MalmoMod.PERFORMANCE_CONFIGS, "outDir", "").getString();
        if(outDir.isEmpty() || outDir == "NONE" ||  ! Files.exists(Paths.get(outDir)) ){
            performanceEnabled = false;
            System.out.println("[LOGTOPY] Performance directory not specified.");
        }
        else{
            performanceEnabled = true;
        }
    }

    static public String getOutDir(){
        return outDir;
    }

    static public boolean performanceEnabled(){
        return performanceEnabled;
    }
}