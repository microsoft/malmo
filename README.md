# Malmö #

Project Malmö is a platform for Artificial Intelligence experimentation and research built on top of Minecraft. We aim to inspire a new generation of research into challenging new problems presented by this unique environment.

[![Join the chat at https://gitter.im/Microsoft/malmo](https://badges.gitter.im/Microsoft/malmo.svg)](https://gitter.im/Microsoft/malmo?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) [![Build Status](https://travis-ci.org/Microsoft/malmo.svg?branch=master)](https://travis-ci.org/Microsoft/malmo) [![license](https://img.shields.io/github/license/mashape/apistatus.svg?maxAge=2592000)](https://github.com/Microsoft/malmo/blob/master/LICENSE.txt)
----
    
## Getting Started ##

प्रोजेक्ट माल्मो मार्टक्राफ्ट के शीर्ष पर बने आर्टिफिशियल इंटेलिजेंस प्रयोग और शोध के लिए एक मंच है। हमारा उद्देश्य इस अनूठे माहौल द्वारा प्रस्तुत चुनौतीपूर्ण नई समस्याओं में अनुसंधान की एक नई पीढ़ी को प्रेरित करना है।

[! [Https://gitter.im/Microsoft/malmo] पर चैट में शामिल हों (https://badges.gitter.im/Microsoft/malmo.svg) ](https://gitter.im/Microsoft/malmo? utm_source = बैज और utm_medium = बैज और utm_campaign = pr-badge और utm_content = बैज) [! [स्थिति बनाएं] (https://travis-ci.org/Microsoft/malmo.svg?branch=master)] (https://travis-ci.org / माइक्रोसॉफ्ट / माल्मो) [! [लाइसेंस] (https://img.shields.io/github/license/mashape/apistatus.svg?maxAge=2592000)] (https://github.com/Microsoft/malmo/blob /master/LICENSE.txt)
----
    
## शुरू करना ##

माल्मो को पाइथन पैकेज के रूप में स्थापित करने के लिए अब `` `pip3 इंस्टॉल माल्मो``` का उपयोग करना संभव है: [माल्मो के लिए पिप इंस्टॉल करें] (https://github.com/Microsoft/malmo/blob/master/scripts/python- पहिया / README.md)। एक बार इंस्टॉल हो जाने पर, माल्मो पायथन मॉड्यूल का उपयोग स्रोत और उदाहरण डाउनलोड करने और माल्मो गेम मोड के साथ Minecraft शुरू करने के लिए किया जा सकता है। वैकल्पिक रूप से, माल्मो का पूर्व-निर्मित संस्करण निम्नानुसार स्थापित किया जा सकता है:

1. [विंडोज, लिनक्स या मैकोज़क्स के लिए नवीनतम * प्री-बिल्ट * संस्करण डाउनलोड करें।] (Https://github.com/Microsoft/malmo/releases)
      नोट: यह _not_ है जो गितूब से स्रोत के ज़िप को डाउनलोड करने जैसा ही है। यह ** ** कार्य नहीं करेगा ** जब तक आप स्रोत कोड स्वयं बनाने की योजना बना रहे हैं (जो एक लंबी प्रक्रिया है)। यदि आपको "आयात त्रुटि: माल्मोपीथन नामक कोई मॉड्यूल नहीं है" के रूप में त्रुटियां मिलती हैं, तो शायद यह इसलिए होगा क्योंकि आपने यह गलती की है ._

2. अपने ओएस के लिए निर्भरता स्थापित करें: [विंडोज़] (डॉक्टर / install_windows.md), [लिनक्स] (डॉक्टर / install_linux.md), [मैकोज़क्स] (डॉक्टर / install_macosx.md)।

3. हमारे मॉड स्थापित के साथ Minecraft लॉन्च करें। नीचे निर्देश।

4. पाइथन, सी #, सी ++ या जावा के रूप में हमारे नमूना एजेंटों में से एक लॉन्च करें। नीचे निर्देश।

5. [ट्यूटोरियल] का पालन करें (https://github.com/Microsoft/malmo/blob/master/Malmo/samples/Python_examples/Tutorial.pdf)

6. [दस्तावेज़ीकरण] का अन्वेषण करें (http://microsoft.github.io/malmo/)। यह रिलीज ज़िप में readme.html में भी उपलब्ध है।

7. अधिक जानकारी के लिए [ब्लॉग] (http://microsoft.github.io/malmo/blog) पढ़ें।

यदि आप स्रोत से निर्माण करना चाहते हैं तो अपने ओएस के लिए निर्माण निर्देश देखें: [विंडोज़] (डॉक्टर / build_windows.md), [लिनक्स] (डॉक्टर / build_linux.md), [मैकोज़क्स] (डॉक्टर / build_macosx.md)।

----

## Problems: ##

We're building up a [Troubleshooting](https://github.com/Microsoft/malmo/wiki/Troubleshooting) page of the wiki for frequently encountered situations. If that doesn't work then please ask a question on our [chat page](https://gitter.im/Microsoft/malmo) or open a [new issue](https://github.com/Microsoft/malmo/issues/new).

----

## Launching Minecraft with our Mod: ##

Minecraft needs to create windows and render to them with OpenGL, so the machine you do this from must have a desktop environment.

Go to the folder where you unzipped the release, then:

`cd Minecraft`  
`launchClient` (On Windows)  
`./launchClient.sh` (On Linux or MacOSX)

or, e.g. `launchClient -port 10001` to launch Minecraft on a specific port.

on Linux or MacOSX: `./launchClient.sh -port 10001`

*NB: If you run this from a terminal, the bottom line will say something like "Building 95%" - ignore this - don't wait for 100%! As long as a Minecraft game window has opened and is displaying the main menu, you are good to go.*

By default the Mod chooses port 10000 if available, and will search upwards for a free port if not, up to 11000.
The port chosen is shown in the Mod config page.

To change the port while the Mod is running, use the `portOverride` setting in the Mod config page.

The Mod and the agents use other ports internally, and will find free ones in the range 10000-11000 so if administering
a machine for network use these TCP ports should be open.

----

## Launch an agent: ##

#### Running a Python agent: ####

```
cd Python_Examples
python3 run_mission.py
``` 

#### Running a C++ agent: ####

`cd Cpp_Examples`

To run the pre-built sample:

`run_mission` (on Windows)  
`./run_mission` (on Linux or MacOSX)

To build the sample yourself:

`cmake .`  
`cmake --build .`  
`./run_mission` (on Linux or MacOSX)  
`Debug\run_mission.exe` (on Windows)

#### Running a C# agent: ####

To run the pre-built sample (on Windows):

`cd CSharp_Examples`  
`CSharpExamples_RunMission.exe`

To build the sample yourself, open CSharp_Examples/RunMission.csproj in Visual Studio.

Or from the command-line:

`cd CSharp_Examples`

Then, on Windows:  
```
msbuild RunMission.csproj /p:Platform=x64
bin\x64\Debug\CSharpExamples_RunMission.exe
```

#### Running a Java agent: ####

`cd Java_Examples`  
`java -cp MalmoJavaJar.jar:JavaExamples_run_mission.jar -Djava.library.path=. JavaExamples_run_mission` (on Linux or MacOSX)  
`java -cp MalmoJavaJar.jar;JavaExamples_run_mission.jar -Djava.library.path=. JavaExamples_run_mission` (on Windows)

#### Running an Atari agent: (Linux only) ####

```
cd Python_Examples
python3 ALE_HAC.py
```

----

# Citations #

Please cite Malmo as:

Johnson M., Hofmann K., Hutton T., Bignell D. (2016) [_The Malmo Platform for Artificial Intelligence Experimentation._](http://www.ijcai.org/Proceedings/16/Papers/643.pdf) [Proc. 25th International Joint Conference on Artificial Intelligence](http://www.ijcai.org/Proceedings/2016), Ed. Kambhampati S., p. 4246. AAAI Press, Palo Alto, California USA. https://github.com/Microsoft/malmo

----

# Code of Conduct #

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/). For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.
