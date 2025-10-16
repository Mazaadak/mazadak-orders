package com.mazadak.orders.dto.client;

import com.mazadak.orders.model.enumeration.Gender;

import java.util.List;

public record UserProfileResponse(String phone,
                                  String aboutMe,
                                  List<AddressDto> addresses,
                                  Gender gender,
                                  String firstName,
                                  String lastName) {
}
