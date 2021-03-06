package com.tirthal.learning.services.review;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.ObservableExecutionMode;

import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

@Service
public class ReviewIntegrationService {

	private static final Logger _log = LoggerFactory.getLogger(ReviewIntegrationService.class);
	
	@Autowired
	RestTemplate restTemplate;
	
	@Autowired
	Environment env;

	@HystrixCommand(fallbackMethod="getReviewsFallback", observableExecutionMode=ObservableExecutionMode.EAGER)
	public Observable<List<Review>> getReviews(final String glId) {
		return Observable.create(new Observable.OnSubscribe<List<Review>>() {
			@Override
			public void call(Subscriber<? super List<Review>> observer) {
				try {
					if(!observer.isUnsubscribed()) {
						
						_log.info("*** (2) Calling review service");
						ParameterizedTypeReference<List<Review>> responseType = new ParameterizedTypeReference<List<Review>>() {  }; 
						List<Review> reviewResponse = restTemplate.exchange(env.getProperty("gs.games.review.service.rest.endpoint"), HttpMethod.GET, null, responseType, glId).getBody();
						
						// Mocking this a bit time-consuming service call, so fallback method executes via Hystrix
						// Thread.sleep(2000); 
						
						_log.info("*** (2) Got response from review service");
						
						observer.onNext(reviewResponse);													
						observer.onCompleted();
					}
				} catch(Exception e) {					
					observer.onError(e);
				}
			}			
		}).subscribeOn(Schedulers.newThread());		// Changed to non-blocking approach
	}
	
	@SuppressWarnings("unused")
	private Observable<List<Review>> getReviewsFallback(final String glId) {
		List<Review> fallbackReview = new ArrayList<>();
		fallbackReview.add(new Review("NA", "No Review", "No review found!", 0, LocalDate.now().toString()));		
		return Observable.just(fallbackReview);
	}
}