# Stage 1: Build
FROM --platform=linux/amd64 node:20 AS builder
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
ARG EXPO_PUBLIC_API_URL
ENV EXPO_PUBLIC_API_URL=$EXPO_PUBLIC_API_URL
ARG EXPO_PUBLIC_GOOGLE_CLIENT_ID
ENV EXPO_PUBLIC_GOOGLE_CLIENT_ID=$EXPO_PUBLIC_GOOGLE_CLIENT_ID

# Build the SPA
RUN npx expo export -p web

# Stage 2: Serve
FROM --platform=linux/amd64 nginx:alpine
RUN rm -rf /usr/share/nginx/html/*

# Nginx Configuration as a Reverse Proxy with HTTPS support
RUN echo 'server { \
    listen 80; \
    server_name swipelab.cs.bgu.ac.il; \
    return 301 https://$host$request_uri; \
} \
server { \
    listen 443 ssl; \
    server_name swipelab.cs.bgu.ac.il; \
    \
    ssl_certificate /etc/letsencrypt/live/swipelab.cs.bgu.ac.il/fullchain.pem; \
    ssl_certificate_key /etc/letsencrypt/live/swipelab.cs.bgu.ac.il/privkey.pem; \
    \
    # Proxy requests to STARdbi to bypass CORS \
    location /stardbi/ { \
        proxy_pass https://stardbi.cs.bgu.ac.il/; \
        proxy_set_header Host stardbi.cs.bgu.ac.il; \
        proxy_ssl_server_name on; \
    } \
    \
    # Route all requests starting with /api to the backend \
    location /api/ { \
        proxy_pass http://swipelab-backend:8080; \
        proxy_set_header Host $host; \
        proxy_set_header X-Real-IP $remote_addr; \
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for; \
        proxy_set_header X-Forwarded-Proto $scheme; \
    } \
    # Route all other requests to the frontend app \
    location / { \
        root /usr/share/nginx/html; \
        index index.html index.htm; \
        try_files $uri $uri/ /index.html; \
    } \
}' > /etc/nginx/conf.d/default.conf

COPY --from=builder /app/dist/ /usr/share/nginx/html/
RUN chmod -R 755 /usr/share/nginx/html
EXPOSE 80 443
CMD ["nginx", "-g", "daemon off;"]
