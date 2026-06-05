# ============================================================
# BillMyGym
# ============================================================

ssh -i incial-key.pem ec2-user@3.6.134.25

#DB Tunnel

cd .\Downloads\
ssh -i elyndorlabs.pem -L 5433:elyndorlabs-db.cf6kgsg80y2k.ap-south-1.rds.amazonaws.com:5432 ec2-user@13.127.80.218

psql -h elyndorlabs-db.cf6kgsg80y2k.ap-south-1.rds.amazonaws.com -U postgres -d billmygym

# ----------------------------
# ENVIRONMENT VARIABLES
# ----------------------------

# Edit environment file
nano billmygym.env

# Restart container after env change
docker restart billmygym

# ----------------------------
# NGINX (IF USED)
# ----------------------------

# Check nginx status
sudo systemctl status nginx

# Test & reload nginx config
sudo nginx -t
sudo systemctl reload nginx

# Restart nginx
sudo systemctl restart nginx

# View nginx logs
cd /etc/nginx/conf.d

# Clear Docker Logs
sudo truncate -s 0 $(docker inspect --format='{{.LogPath}}' f24beff57720)


# Check env inside container
docker exec billmygym env
docker stats --no-stream



aws ecr get-login-password --region ap-south-1 \
| docker login --username AWS --password-stdin \
811165582901.dkr.ecr.ap-south-1.amazonaws.com/billmygym


docker stop billmygym
docker rm -f billmygym

docker pull 811165582901.dkr.ecr.ap-south-1.amazonaws.com/billmygym:latest

docker run -d \
  --name billmygym \
  --restart unless-stopped \
  --env-file /home/ec2-user/billmygym.env \
  -p 8083:8083 \
  --memory="600m" \
  --memory-swap="800m" \
  -e JAVA_OPTS="-Xms128m -Xmx400m" \
  811165582901.dkr.ecr.ap-south-1.amazonaws.com/billmygym:latest

docker logs -f billmygym





#DB Tunnel

cd .\Downloads\
ssh -i elyndorlabs.pem -L 5433:elyndorlabs-db.cf6kgsg80y2k.ap-south-1.rds.amazonaws.com:5432 ec2-user@13.127.80.218

psql -h elyndorlabs-db.cf6kgsg80y2k.ap-south-1.rds.amazonaws.com -U postgres -d billmygym


docker stop billmygym
docker rm -f billmygym


docker run -d \
  --name billmygym \
  --restart unless-stopped \
  --env-file /home/ec2-user/billmygym.env \
  -p 8080:8080 \
  --memory="600m" \
  --memory-swap="800m" \
  -e SPRING_FLYWAY_ENABLED=false \
  -e SPRING_JPA_HIBERNATE_DDL_AUTO=update\
  -e JAVA_OPTS="-Xms128m -Xmx400m" \
  811165582901.dkr.ecr.ap-south-1.amazonaws.com/billmygym:latest

docker logs -f billmygym
