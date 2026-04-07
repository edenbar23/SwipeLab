# Stage 1: Build
FROM --platform=linux/amd64 node:20 AS builder
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
ARG EXPO_PUBLIC_API_URL
ENV EXPO_PUBLIC_API_URL=$EXPO_PUBLIC_API_URL

# בנייה של ה-SPA
RUN npx expo export -p web

# Stage 2: Serve
FROM --platform=linux/amd64 nginx:alpine
RUN rm -rf /usr/share/nginx/html/*

# קונפיגורציה של Nginx כ-Reverse Proxy
RUN echo 'server { \
    listen 80; \
    # כל בקשה שמתחילה ב-api תעבור לבקאנד
    location /api/ { \
        proxy_pass http://swipelab-backend:8080; \
        proxy_set_header Host $host; \
        proxy_set_header X-Real-IP $remote_addr; \
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for; \
        proxy_set_header X-Forwarded-Proto $scheme; \
    } \
    # שאר הבקשות ילכו לאתר
    location / { \
        root /usr/share/nginx/html; \
        index index.html index.htm; \
        try_files $uri $uri/ /index.html; \
    } \
}' > /etc/nginx/conf.d/default.conf

COPY --from=builder /app/dist/ /usr/share/nginx/html/
RUN chmod -R 755 /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
