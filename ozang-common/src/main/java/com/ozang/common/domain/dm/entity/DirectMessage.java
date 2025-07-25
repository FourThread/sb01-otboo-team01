package com.fourthread.ozang.module.domain.dm.entity;

import com.fourthread.ozang.module.domain.BaseEntity;
import com.fourthread.ozang.module.domain.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@Entity
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "direct_messages")
public class DirectMessage extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  private User sender;

  @ManyToOne(fetch = FetchType.LAZY)
  private User receiver;

  private String content;
  private String dmKey;

  public static String generatedDmKey(User sender, User receiver) {
    String senderId = sender.getId().toString();
    String receiverId = receiver.getId().toString();

    if (senderId.compareTo(receiverId) < 0) {
      return senderId + "_" + receiverId;
    }
    return receiverId + "_" + senderId;
  }
}
