
/*
 *
 * * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.services;

import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.exchanges.GetRestaurantsRequest;
import com.crio.qeats.exchanges.GetRestaurantsResponse;
import com.crio.qeats.models.RestaurantEntity;
import com.crio.qeats.repositoryservices.RestaurantRepositoryService;
import com.crio.qeats.repositoryservices.RestaurantRepositoryServiceImpl;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class RestaurantServiceImpl implements RestaurantService {

  private final Double peakHoursServingRadiusInKms = 3.0;
  private final Double normalHoursServingRadiusInKms = 5.0;

  @Autowired
  // @Qualifier("RestaurantRepositoryServiceImpl")
  private RestaurantRepositoryService restaurantRepositoryService;



  // TODO: CRIO_TASK_MODULE_RESTAURANTSAPI - Implement findAllRestaurantsCloseby.
  // Check RestaurantService.java file for the interface contract.
  @Override
  public GetRestaurantsResponse findAllRestaurantsCloseBy(
      GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {
    Double latitude = getRestaurantsRequest.getLatitude();
    Double longitude = getRestaurantsRequest.getLongitude();
    if (currentTime.isAfter(LocalTime.of(7, 59)) && currentTime.isBefore(LocalTime.of(10, 1))
        || currentTime.isAfter(LocalTime.of(12, 59)) && currentTime.isBefore(LocalTime.of(14, 1))
        || currentTime.isAfter(LocalTime.of(18, 59)) && currentTime.isBefore(LocalTime.of(21, 1))) {
      List<Restaurant> allRestaurants = restaurantRepositoryService
          .findAllRestaurantsCloseBy(latitude, longitude, currentTime, peakHoursServingRadiusInKms);
      return new GetRestaurantsResponse(allRestaurants);
    } else {
      List<Restaurant> allRestaurants = restaurantRepositoryService.findAllRestaurantsCloseBy(
          latitude, longitude, currentTime, normalHoursServingRadiusInKms);
      return new GetRestaurantsResponse(allRestaurants);
    }
  }



  // @Override
  // public GetRestaurantsResponse findAllRestaurantsCloseBy(
  // GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {


  // }
  private boolean isPeakHour(LocalTime currentTime) {
    if (currentTime.isAfter(LocalTime.of(7, 59)) && currentTime.isBefore(LocalTime.of(10, 1))
        || currentTime.isAfter(LocalTime.of(12, 59)) && currentTime.isBefore(LocalTime.of(14, 1))
        || currentTime.isAfter(LocalTime.of(18, 59)) && currentTime.isBefore(LocalTime.of(21, 1))) {
      return true;
    }
    return false;
  }



  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Implement findRestaurantsBySearchQuery. The request object has the search string.
  // We have to combine results from multiple sources:
  // 1. Restaurants by name (exact and inexact)
  // 2. Restaurants by cuisines (also called attributes)
  // 3. Restaurants by food items it serves
  // 4. Restaurants by food item attributes (spicy, sweet, etc)
  // Remember, a restaurant must be present only once in the resulting list.
  // Check RestaurantService.java file for the interface contract.
  @Override
  public GetRestaurantsResponse findRestaurantsBySearchQuery(
      GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {

    Double servingRadiusInKms =
        isPeakHour(currentTime) ? peakHoursServingRadiusInKms : normalHoursServingRadiusInKms;
    String searchFor = getRestaurantsRequest.getSearchFor();
    List<List<Restaurant>> listOfRestaurantLists = new ArrayList<>();
    if (!searchFor.isEmpty()) {
      listOfRestaurantLists.add(
          restaurantRepositoryService.findRestaurantsByName(getRestaurantsRequest.getLatitude(),

              getRestaurantsRequest.getLongitude(), searchFor, currentTime,

              servingRadiusInKms));

      listOfRestaurantLists.add(restaurantRepositoryService.findRestaurantsByAttributes(
          getRestaurantsRequest.getLatitude(),

          getRestaurantsRequest.getLongitude(), searchFor,

          currentTime, servingRadiusInKms));
      listOfRestaurantLists.add(
          restaurantRepositoryService.findRestaurantsByItemName(getRestaurantsRequest.getLatitude(),

              getRestaurantsRequest.getLongitude(), searchFor,

              currentTime, servingRadiusInKms));
      listOfRestaurantLists.add(restaurantRepositoryService.findRestaurantsByItemAttributes(
          getRestaurantsRequest.getLatitude(),

          getRestaurantsRequest.getLongitude(), searchFor,

          currentTime, servingRadiusInKms));
      Set<String> restaurantSet = new HashSet<>();
      List<Restaurant> restaurantList = new ArrayList<>();
      for (List<Restaurant> restoList : listOfRestaurantLists) {
        for (Restaurant restaurant : restoList) {
          if (!restaurantSet.contains(restaurant.getRestaurantId())) {
            restaurantList.add(restaurant);
            restaurantSet.add(restaurant.getRestaurantId());
          }
        }
      }
      return new GetRestaurantsResponse(restaurantList);
    } else {
      return new GetRestaurantsResponse(new ArrayList<>());
    }
  }



  // TODO: CRIO_TASK_MODULE_MULTITHREADING
  // Implement multi-threaded version of RestaurantSearch.
  // Implement variant of findRestaurantsBySearchQuery which is at least 1.5x time faster than
  // findRestaurantsBySearchQuery.
  @Override
  public GetRestaurantsResponse findRestaurantsBySearchQueryMt(
      GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {

    Double servingRadiusInKms =
        isPeakHour(currentTime) ? peakHoursServingRadiusInKms : normalHoursServingRadiusInKms;
    String searchFor = getRestaurantsRequest.getSearchFor();
    List<Restaurant> restaurantList;

    if (!searchFor.isEmpty()) {
      long startTime = System.currentTimeMillis();
      Future<List<Restaurant>> futureGetRestaurantsByNameList = restaurantRepositoryService
          .findRestaurantsByNameAsync(getRestaurantsRequest.getLatitude(),
              getRestaurantsRequest.getLongitude(), searchFor, currentTime, servingRadiusInKms);
      Future<List<Restaurant>> futureGetRestaurantsByAttributesList = restaurantRepositoryService
          .findRestaurantsByAttributesAsync(getRestaurantsRequest.getLatitude(),
              getRestaurantsRequest.getLongitude(), searchFor, currentTime, servingRadiusInKms);
      List<Restaurant> restaurantsByNameList;
      List<Restaurant> restaurantByAttributesList;
      try {
        while (true) {
          if (futureGetRestaurantsByNameList.isDone()
              && futureGetRestaurantsByAttributesList.isDone()) {
            restaurantsByNameList = futureGetRestaurantsByNameList.get();
            restaurantByAttributesList = futureGetRestaurantsByAttributesList.get();

            log.info("Time in millis: " + (System.currentTimeMillis() - startTime));
            break;
          }
        }
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
        return new GetRestaurantsResponse(new ArrayList<>());
      }

      Map<String, Restaurant> restaurantMap = new HashMap<>();
      for (Restaurant restaurant : restaurantsByNameList) {
        restaurantMap.put(restaurant.getRestaurantId(), restaurant);
      }
      for (Restaurant restaurant : restaurantByAttributesList) {
        restaurantMap.put(restaurant.getRestaurantId(), restaurant);
      }
      restaurantList = new ArrayList<>(restaurantMap.values());
    } else {
      restaurantList = new ArrayList<>();
    }
    return new GetRestaurantsResponse(restaurantList);


  }


}

