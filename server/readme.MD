# Server deployment instruction

Just push to the master branch, the script will publish your artifact to our server. 
Avoid pushing wrong or inconsistent code to the master branch.
Always test you current software with a local docker build.
```bash
docker build -t cTracker:latest .
docker run -d -p 4567:4567 --name tracker cTracker:latest
docker ps
```
The console should show a running container with the name ```tracker```. 

Current reachable endpoint https://tracing.uniks.de/hello


## Troubleshooting
Some quick fixes
### My server is not reachable?
- Maybe you changed the port in your application, you need to change the ```EXPOSE``` line in the ```Dockerfile```.
- You pushed some strange code therefore the container could be broken. Test your build locally!
