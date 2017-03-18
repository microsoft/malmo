# GoWrapper for Malmo

## Steps

1. Run:

```
xsd cxx-tree --generate-polymorphic --namespace-map http://ProjectMalmo.microsoft.com=malmo::schemas --root-element Mission --root-element MissionInit --root-element MissionEnded --root-element Reward --generate-serialization --hxx-suffix .h --cxx-suffix .cpp *.xsd
```
