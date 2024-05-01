module image {
   exports com.udacity.image.service;
   requires software.amazon.awssdk.regions;
   requires slf4j.api;
   requires software.amazon.awssdk.auth;
   requires software.amazon.awssdk.services.rekognition;
   requires software.amazon.awssdk.core;
   requires java.desktop;
}