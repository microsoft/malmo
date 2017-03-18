# GoWrapper for Malmo

## Steps

1. Run XSD on Schemas files

```
xsd cxx-tree --generate-polymorphic --namespace-map http://ProjectMalmo.microsoft.com=malmo::schemas --root-element Mission --root-element MissionInit --root-element MissionEnded --root-element Reward --generate-serialization --hxx-suffix .h --cxx-suffix .cpp *.xsd
```

2. Link directory malmo/Malmo/src/GoWrapper to $GOPATH/src
