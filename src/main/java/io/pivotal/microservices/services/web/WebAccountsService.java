package io.pivotal.microservices.services.web;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import io.pivotal.microservices.exceptions.AccountNotFoundException;
import io.pivotal.microservices.services.util.SkipSSLVerifyRestTemplate;

/**
 * Hide the access to the microservice inside this local service.
 * 
 * @author Paul Chapman
 */
@Service
public class WebAccountsService {

	@Autowired
	@LoadBalanced
	protected RestTemplate restTemplate;

	@Value("${service.accounts.url:''}")
	protected String serviceUrl;

	@Value("${service.accounts.protocol:'http'}")
	protected String serviceProtocol;

	@Value("${service.accounts.skip_ssl_verification:false}")
	protected boolean serviceSkipSSLVerification;

	protected Logger logger = Logger.getLogger(WebAccountsService.class.getName());

	@PostConstruct
	public void init() {
		this.serviceUrl = serviceUrl.startsWith("http") ? serviceUrl
				: String.format("%s://%s", serviceProtocol, serviceUrl);
		logger.info("Using accounts service url: " + serviceUrl);
		if ("https".equals(serviceProtocol) && serviceSkipSSLVerification) {
			SkipSSLVerifyRestTemplate.setSkipSSLVerify(restTemplate);
		}
		logger.warning("The RestTemplate request factory is " + restTemplate.getRequestFactory().getClass());
	}

	public Account findByNumber(String accountNumber) {
		logger.info("findByNumber() invoked: for " + accountNumber);
		try {
			Account account = restTemplate.getForObject(serviceUrl + "/accounts/{number}", Account.class,
					accountNumber);
			if (account == null)
				throw new AccountNotFoundException(accountNumber);
			return account;
		} catch (HttpClientErrorException e) {
			if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
				throw new AccountNotFoundException(accountNumber);
			}
			throw e;
		}
	}

	public List<Account> byOwnerContains(String name) {
		logger.info("byOwnerContains() invoked:  for " + name);
		try {
			Account[] accounts = restTemplate.getForObject(serviceUrl + "/accounts/owner/{name}", Account[].class,
					name);
			if (accounts == null || accounts.length == 0)
				return null;
			return Arrays.asList(accounts);
		} catch (HttpClientErrorException e) {
			return null;
		}
	}
}
