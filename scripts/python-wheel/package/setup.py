"""A setuptools based setup module.

See:
https://packaging.python.org/en/latest/distributing.html
https://github.com/pypa/sampleproject
"""

# Always prefer setuptools over distutils
from setuptools import setup, find_packages
# To use a consistent encoding
from codecs import open
from os import path
from distutils.core import Extension
from pathlib import Path
import os
import sys
import glob
import platform

# Arguments marked as "Required" below must be included for upload to PyPI.
# Fields marked as "Optional" may be commented out.

version = Path('VERSION').read_text().strip() 
modversion = version + ".0"
Path('malmo/version.py').write_text('version="{}"'.format(version))

root_dir = "."
malmo_src_dir = os.path.join(root_dir, "src")
malmo_python_sources = [
   "AgentHost.cpp",
   "ArgumentParser.cpp",
   "ErrorCodeSync.cpp",
   "ClientConnection.cpp",
   "ClientInfo.cpp",
   "ClientPool.cpp",
   "Logger.cpp",
   "FindSchemaFile.cpp",
   "RewardXML.cpp",
   "MissionInitXML.cpp",
   "MissionEndedXML.cpp",
   "MissionInitSpec.cpp",
   "MissionRecord.cpp",
   "MissionRecordSpec.cpp",
   "MissionSpec.cpp",
   "ParameterSet.cpp",
   "StringServer.cpp",
   "TCPClient.cpp",
   "TCPConnection.cpp",
   "TCPServer.cpp",
   "TimestampedReward.cpp",
   "TimestampedString.cpp",
   "TimestampedVideoFrame.cpp",
   "VideoFrameWriter.cpp",
   "BmpFrameWriter.cpp",
   "VideoServer.cpp",
   "WorldState.cpp",
]
if os.name == "nt":
    malmo_python_sources.append("WindowsFrameWriter.cpp")
else:
    malmo_python_sources.append("PosixFrameWriter.cpp")

malmo_python_libs = [
    "boost_atomic",
    "boost_chrono",
    "boost_date_time",
    "boost_filesystem",
    "boost_iostreams",
    "boost_program_options",
    f"boost_python{sys.version_info[0]}{sys.version_info[1]}",
    "boost_regex",
    "boost_system",
    "boost_thread",
    "pthread",
    "z",
]
if platform.system() == "Linux":
    malmo_python_libs.append("rt")

include_dirs = [malmo_src_dir]
library_dirs = []
if platform.system() == "Darwin":
    deps_dir = os.path.join(root_dir, "deps")
    include_dirs.append(os.path.join(deps_dir, "include"))
    library_dirs.append(os.path.join(deps_dir, "lib"))

extra_link_args = [f"-l{lib}" for lib in malmo_python_libs]
if platform.system() == "Darwin":
    extra_link_args.append("-headerpad_max_install_names")

malmo_python_extension = Extension(
    "MalmoPython",
    sources=(
        [os.path.join(malmo_src_dir, "PythonWrapper", "python_module.cpp")]
        + [os.path.join(malmo_src_dir, source_file) for source_file in malmo_python_sources]
    ),
    include_dirs=include_dirs,
    library_dirs=library_dirs,
    define_macros=[
        ("MALMO_VERSION", version),
    ],
    extra_compile_args=["-std=c++11"],
    extra_link_args=extra_link_args,
)

setup(
    # This is the name of your project. The first time you publish this
    # package, this name will be registered for you. It will determine how
    # users can install this project, e.g.:
    #
    # $ pip install sampleproject
    #
    # And where it will live on PyPI: https://pypi.org/project/sampleproject/
    #
    # There are some restrictions on what makes a valid project name
    # specification here:
    # https://packaging.python.org/specifications/core-metadata/#name
    name='mbag-malmo',  # Required

    # Versions should comply with PEP 440:
    # https://www.python.org/dev/peps/pep-0440/
    #
    # For a discussion on single-sourcing the version across setup.py and the
    # project code, see
    # https://packaging.python.org/en/latest/single_source_version.html
    # Version format is: Major.Minor.Revision.PlatformRelease
    # Change PlatformRelease when updating PyPi repro.
    version=modversion, # Required

    # This is a one-line description or tagline of what your project does. This
    # corresponds to the "Summary" metadata field:
    # https://packaging.python.org/specifications/core-metadata/#summary
    description='Project Malmo is a platform for Artificial Intelligence experimentation and research built on top of Minecraft',  # Required

    # This is an optional longer description of your project that represents
    # the body of text which users will see when they visit PyPI.
    #
    # Often, this is the same as your README, so you can just read it in from
    # that file directly (as we have already done above)
    #
    # This field corresponds to the "Description" metadata field:
    # https://packaging.python.org/specifications/core-metadata/#description-optional
    # long_description=Path('../README.md').read_text(),  # Optional

    # Denotes that our long_description is in Markdown; valid values are
    # text/plain, text/x-rst, and text/markdown
    #
    # Optional if long_description is written in reStructuredText (rst) but
    # required for plain-text or Markdown; if unspecified, "applications should
    # attempt to render [the long_description] as text/x-rst; charset=UTF-8 and
    # fall back to text/plain if it is not valid rst" (see link below)
    #
    # This field corresponds to the "Description-Content-Type" metadata field:
    # https://packaging.python.org/specifications/core-metadata/#description-content-type-optional
    long_description_content_type='text/markdown',  # Optional (see note above)

    # This should be a valid link to your project's main homepage.
    #
    # This field corresponds to the "Home-Page" metadata field:
    # https://packaging.python.org/specifications/core-metadata/#home-page-optional
    url='https://github.com/cassidylaidlaw/malmo',  # Optional

    # This should be your name or the name of the organization which owns the
    # project.
    author='Cassidy Laidlaw',  # Optional

    # This should be a valid email address corresponding to the author listed
    # above.
    author_email='v-andkra@microsoft.com',  # Optional


    # Include a dummy extension so that a platform specific wheel is built.
    ext_modules=[malmo_python_extension],

    # Classifiers help users find your project by categorizing it.
    #
    # For a list of valid classifiers, see
    # https://pypi.python.org/pypi?%3Aaction=list_classifiers
    classifiers=[  # Optional
        # How mature is this project? Common values are
        #   3 - Alpha
        #   4 - Beta
        #   5 - Production/Stable
        'Development Status :: 4 - Beta',

        # Indicate who your project is intended for
        'Intended Audience :: Developers',
        'Topic :: Scientific/Engineering :: Artificial Intelligence',

        # Pick your license as you wish
        'License :: OSI Approved :: MIT License',

        # Specify the Python versions you support here. In particular, ensure
        # that you indicate whether you support Python 2, Python 3 or both.
        'Programming Language :: Python :: 3.4',
        'Programming Language :: Python :: 3.5',
        'Programming Language :: Python :: 3.6',
    ],

    # This field adds keywords for your project which will appear on the
    # project page. What does your project relate to?
    #
    # Note that this is a string of words separated by whitespace, not a list.
    keywords='AI Minecraft Reinforcement-Learning',  # Optional

    # You can just specify package directories manually here if your project is
    # simple. Or you can use find_packages().
    #
    # Alternatively, if you just want to distribute a single Python file, use
    # the `py_modules` argument instead as follows, which will expect a file
    # called `my_module.py` to exist:
    #
    #   py_modules=["my_module"],
    #
    packages=find_packages(exclude=['contrib', 'docs', 'tests']),  # Required

    # This field lists other packages that your project depends on to run.
    # Any package you put here will be installed by pip when your project is
    # installed, so they must be valid existing projects.
    #
    # For an analysis of "install_requires" vs pip's requirements files see:
    # https://packaging.python.org/en/latest/requirements.html
    #install_requires=['pillow', 'matplotlib'],  # Optional

    # List additional groups of dependencies here (e.g. development
    # dependencies). Users will be able to install these using the "extras"
    # syntax, for example:
    #
    #   $ pip install sampleproject[dev]
    #
    # Similar to `install_requires` above, these must be valid existing
    # projects.
    extras_require={  # Optional
        'dev': ['check-manifest'],
        'test': ['coverage'],
    },

    # If there are data files included in your packages that need to be
    # installed, specify them here.
    #
    # If using Python 2.6 or earlier, then these have to be included in
    # MANIFEST.in as well.
    include_package_data=True,
    package_data={  # Optional
        'malmo': ['Minecraft/**/*', 'Schemas/**/*'],
    },

    # Although 'package_data' is the preferred approach, in some case you may
    # need to place data files outside of your packages. See:
    # http://docs.python.org/3.4/distutils/setupscript.html#installing-additional-files
    #
    # In this case, 'data_file' will be installed into '<sys.prefix>/my_data'
    #data_files=[('my_data', ['data/data_file'])],  # Optional

    # To provide executable scripts, use entry points in preference to the
    # "scripts" keyword. Entry points provide cross-platform support and allow
    # `pip` to create the appropriate form of executable for the target
    # platform.
    #
    # For example, the following would provide a command called `sample` which
    # executes the function `main` from this package when invoked:
    entry_points={  # Optional
    #    'console_scripts': [
    #        'sample=sample:main',
    #    ],
    },

    # List additional URLs that are relevant to your project as a dict.
    #
    # This field corresponds to the "Project-URL" metadata fields:
    # https://packaging.python.org/specifications/core-metadata/#project-url-multiple-use
    #
    # Examples listed include a pattern for specifying where the package tracks
    # issues, where the source is hosted, where to say thanks to the package
    # maintainers, and where to support the project financially. The key is
    # what's used to render the link text on PyPI.
    project_urls={  # Optional
        'Bug Reports': 'https://github.com/Microsoft/malmo/issues',
        'Funding': 'https://donate.pypi.org',
        'Say Thanks!': 'http://saythanks.io/to/example',
        'Source': 'https://github.com/Microsoft/malmo/',
    },
)

