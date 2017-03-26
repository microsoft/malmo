# GoWrapper for Malmo

## Steps

1. Run XSD on Schemas files

```
cd malmo/Schemas
xsd cxx-tree --generate-polymorphic --namespace-map http://ProjectMalmo.microsoft.com=malmo::schemas --root-element Mission --root-element MissionInit --root-element MissionEnded --root-element Reward --generate-serialization --hxx-suffix .h --cxx-suffix .cpp *.xsd
```

2. Link directory malmo/Malmo/src/GoWrapper to $GOPATH/src
```
ln -s $MALMO_DRIVE/malmo/Malmo/src/GoWrapper $GOPATH/src
```

## Filenames

Files named with `x_*.{h,cpp}` implement a lightweight wrapper in pure C language.

Files named with `t_*.go` are unit tests.

## Notes

`AgentHost` is the _main_ file in the sense that it contais the CXXFLAGS and the LDFLAGS
definitions. The file named `auxiliary.h` contains other functions to deal with allocation of
chars in a _pure_ C style.

`Go` used to have problems with passing `bool` values from/to C. Therefore, this wrapper converts
all logic flags (`bools`) to `int`.
