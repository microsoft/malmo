import setuptools

with open("README.md", "r") as fh:
    long_description = fh.read()

setuptools.setup(
    name="malmoenv",
    version="0.0.1",
    author="Andre Kramer",
    author_email="v-andkra@microsoft.com",
    description="A gym environemnt for Malmo",
    long_description=long_description,
    long_description_content_type="text/markdown",
    url="https://github.com/Microsoft/malmo",
    install_requires=['gym', 'lxml', 'pillow'],
    packages=setuptools.find_packages(),
    classifiers=[
        "Programming Language :: Python :: 3",
        "License :: OSI Approved :: MIT License",
        "Operating System :: OS Independent",
    ],
)
