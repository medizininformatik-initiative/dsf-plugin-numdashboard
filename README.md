# NUM Dashboard Report

In this repository you will find the process to send reports of current FHIR store implementation progress of DICs to a HRP.

## License

This project is released under the terms of [GPL version 3](LICENSE.md).

```
This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
```

## Documentation

The documentation of the process including the description, the deployment and configuration guides as well as instructions on how to start a process instance can be found in the [wiki](https://github.com/medizininformatik-initiative/NUM-Dashboard-Report/wiki).

## Build

Prerequisite: Java 17, Maven >= 3.6

To use this repository in your code, add the Github Package Registry server to your Maven `.m2/settings.xml`. Instructions on how to generate the `USERNAME` and `TOKEN` can be found in the GitHub documentation [Managing your personal access tokens](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens). The token needs at least the `read:packages` scope.

```xml
<servers>
    <server>
        <id>github-mii</id>
        <username>USERNAME</username>
        <password>TOKEN</password>
    </server>
</servers>
```
