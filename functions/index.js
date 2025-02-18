const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

const notificationSound = {
  apns: {
    payload: {
      aps: {
        sound: "notification_sound.caf",
      },
    },
  },
};

exports.sendOfferNotification = functions.firestore
  .document("offers/{offerId}")
  .onCreate(async (snap, context) => {
    const newOffer = snap.data();
    const listingId = newOffer.listingID;
    console.log("New offer created for listing:", listingId);

    try {
      const listingDoc = await admin.firestore().collection("listings").doc(listingId).get();
      if (!listingDoc.exists) {
        console.log("Listing not found");
        return;
      }
      const listing = listingDoc.data();

      const ownerDoc = await admin.firestore().collection("users").doc(listing.ownerID).get();
      if (!ownerDoc.exists) {
        console.log("Owner not found");
        return;
      }
      const ownerData = ownerDoc.data();

      const notificationPromises = [];

      // İlan sahibine bildirim gönderme ve kaydetme
      if (ownerData.fcmToken) {
        // Firestore'a bildirim kaydı
        notificationPromises.push(
          admin.firestore().collection("users").doc(listing.ownerID)
            .collection("notifications")
            .add({
              title: "Yeni Teklif",
              message: `${listing.title} ilanınıza yeni bir teklif geldi.`,
              type: "newOffer",
              relatedID: listingId,
              timestamp: admin.firestore.FieldValue.serverTimestamp(),
              isRead: false,
            }),
        );

        // FCM bildirimi
        notificationPromises.push(
          admin.messaging().send({
            notification: {
              title: "Yeni Teklif",
              body: `${listing.title} ilanınıza yeni bir teklif geldi.`,
            },
            data: {
              type: "listing",
              listingID: listingId,
              offerID: context.params.offerId,
            },
            token: ownerData.fcmToken,
            ...notificationSound,
          }),
        );
      }

      // Admin bildirimi ve kaydı
      notificationPromises.push(
        admin.firestore().collection("adminNotifications").add({
          title: "Yeni Teklif",
          message: `${listing.title} ilanına yeni bir teklif geldi.`,
          type: "newOffer",
          relatedID: listingId,
          timestamp: admin.firestore.FieldValue.serverTimestamp(),
          isRead: false,
        }),
      );

      notificationPromises.push(
        admin.messaging().send({
          notification: {
            title: "Yeni Teklif",
            body: `${listing.title} ilanına yeni bir teklif geldi.`,
          },
          data: {
            type: "offer",
            listingID: listingId,
            offerID: context.params.offerId,
          },
          topic: "admin",
          ...notificationSound,
        }),
      );

      // Daha düşük teklifleri olan kullanıcılara bildirim
      const lowerOffers = await admin.firestore()
        .collection("offers")
        .where("listingID", "==", listingId)
        .where("offerPrice", "<", newOffer.offerPrice)
        .get();

      for (const doc of lowerOffers.docs) {
        const offer = doc.data();
        if (offer.userID !== newOffer.userID) {
          const userDoc = await admin.firestore().collection("users").doc(offer.userID).get();
          const userData = userDoc.data();
          if (userData && userData.fcmToken) {
            // Firestore'a bildirim kaydı
            notificationPromises.push(
              admin.firestore().collection("users").doc(offer.userID)
                .collection("notifications")
                .add({
                  title: "Daha Yüksek Teklif",
                  message: `Teklif verdiğiniz ${listing.title} ilanına daha yüksek bir teklif verildi.`,
                  type: "offerUpdate",
                  relatedID: listingId,
                  timestamp: admin.firestore.FieldValue.serverTimestamp(),
                  isRead: false,
                }),
            );

            // FCM bildirimi
            notificationPromises.push(
              admin.messaging().send({
                notification: {
                  title: "Daha Yüksek Teklif",
                  body: `Teklif verdiğiniz ${listing.title} ilanına daha yüksek bir teklif verildi.`,
                },
                data: {
                  type: "listing",
                  listingID: listingId,
                  offerID: context.params.offerId,
                },
                token: userData.fcmToken,
                ...notificationSound,
              }),
            );
          }
        }
      }

      await Promise.all(notificationPromises);
      console.log("All offer notifications sent successfully");
    } catch (error) {
      console.error("Error in sendOfferNotification:", error);
    }
  });

exports.sendNewListingNotification = functions.firestore
  .document("listings/{listingId}")
  .onCreate(async (snap, context) => {
    const newListing = snap.data();
    const listingId = context.params.listingId;
    console.log("New listing created:", newListing.title);

    try {
      // Admin bildirimi ve kaydı
      const notificationPromises = [
        // Firestore'a kayıt
        admin.firestore().collection("adminNotifications").add({
          title: "Yeni İlan Onayı",
          message: `Onaylanacak yeni bir ilan var: ${newListing.title}`,
          type: "newListing",
          relatedID: listingId,
          timestamp: admin.firestore.FieldValue.serverTimestamp(),
          isRead: false,
        }),
        // FCM bildirimi
        admin.messaging().send({
          notification: {
            title: "Yeni İlan Onayı",
            body: `Onaylanacak yeni bir ilan var: ${newListing.title}`,
          },
          data: {
            type: "listing",
            listingID: listingId,
          },
          topic: "admin",
          ...notificationSound,
        }),
      ];

      await Promise.all(notificationPromises);
      console.log("Admin notification sent for new listing");
    } catch (error) {
      console.error("Error in sendNewListingNotification:", error);
    }
  });

exports.sendListingApprovedNotification = functions.firestore
  .document("listings/{listingId}")
  .onUpdate(async (change, context) => {
    const afterUpdate = change.after.data();
    const beforeUpdate = change.before.data();
    const listingId = context.params.listingId;

    if (beforeUpdate.status === "pending" && afterUpdate.status === "approved") {
      try {
        const notificationPromises = [];

        // İlan sahibine bildirim
        const ownerDoc = await admin.firestore().collection("users").doc(afterUpdate.ownerID).get();
        if (ownerDoc.exists) {
          const ownerData = ownerDoc.data();
          if (ownerData.fcmToken) {
            notificationPromises.push(
              // Firestore'a bildirim kaydı
              admin.firestore().collection("users").doc(afterUpdate.ownerID)
                .collection("notifications")
                .add({
                  title: "İlan Onaylandı",
                  message: `${afterUpdate.title} ilanınız onaylandı.`,
                  type: "listingApproved",
                  relatedID: listingId,
                  timestamp: admin.firestore.FieldValue.serverTimestamp(),
                  isRead: false,
                }),
              // FCM bildirimi
              admin.messaging().send({
                notification: {
                  title: "İlan Onaylandı",
                  body: `${afterUpdate.title} ilanınız onaylandı.`,
                },
                data: {
                  type: "listing",
                  listingID: listingId,
                },
                token: ownerData.fcmToken,
                ...notificationSound,
              }),
            );
          }
        }

        // İlgili talep sahiplerine bildirim
        const normalizedCity = afterUpdate.city.toLowerCase().trim();
        const allRequests = await admin.firestore().collection("requests").get();

        for (const doc of allRequests.docs) {
          const request = doc.data();
          const cityMatch = !request.city ||
            request.city.toLowerCase().trim() === normalizedCity ||
            normalizedCity.includes(request.city.toLowerCase().trim());
          const categoryMatch = !request.category || request.category === afterUpdate.category;

          if (cityMatch && categoryMatch) {
            const userDoc = await admin.firestore().collection("users").doc(request.userID).get();
            const userData = userDoc.data();
            if (userData && userData.fcmToken) {
              notificationPromises.push(
                // Firestore'a bildirim kaydı
                admin.firestore().collection("users").doc(request.userID)
                  .collection("notifications")
                  .add({
                    title: "Eşleşen İlan",
                    message: `Talebinize uygun yeni bir ilan: ${afterUpdate.title}`,
                    type: "matchingListing",
                    relatedID: listingId,
                    timestamp: admin.firestore.FieldValue.serverTimestamp(),
                    isRead: false,
                  }),
                // FCM bildirimi
                admin.messaging().send({
                  notification: {
                    title: "Eşleşen İlan",
                    body: `Talebinize uygun yeni bir ilan: ${afterUpdate.title}`,
                  },
                  data: {
                    type: "listing",
                    listingID: listingId,
                  },
                  token: userData.fcmToken,
                  ...notificationSound,
                }),
              );
            }
          }
        }

        await Promise.all(notificationPromises);
        console.log("All approval notifications sent successfully");
      } catch (error) {
        console.error("Error in sendListingApprovedNotification:", error);
      }
    }
  });

exports.sendListingRejectedNotification = functions.firestore
  .document("listings/{listingId}")
  .onUpdate(async (change, context) => {
    const afterUpdate = change.after.data();
    const beforeUpdate = change.before.data();
    const listingId = context.params.listingId;

    if (beforeUpdate.status === "pending" && afterUpdate.status === "rejected") {
      try {
        const ownerDoc = await admin.firestore().collection("users").doc(afterUpdate.ownerID).get();
        if (ownerDoc.exists) {
          const ownerData = ownerDoc.data();
          if (ownerData.fcmToken) {
            const notificationPromises = [
              // Firestore'a bildirim kaydı
              admin.firestore().collection("users").doc(afterUpdate.ownerID)
                .collection("notifications")
                .add({
                  title: "İlan Reddedildi",
                  message: `${afterUpdate.title} ilanınız reddedildi.`,
                  type: "listingRejected",
                  relatedID: listingId,
                  timestamp: admin.firestore.FieldValue.serverTimestamp(),
                  isRead: false,
                }),
              // FCM bildirimi
              admin.messaging().send({
                notification: {
                  title: "İlan Reddedildi",
                  body: `${afterUpdate.title} ilanınız reddedildi.`,
                },
                data: {
                  type: "listing",
                  listingID: listingId,
                },
                token: ownerData.fcmToken,
                ...notificationSound,
              }),
            ];

            await Promise.all(notificationPromises);
            console.log("Rejection notifications sent successfully");
          }
        }
      } catch (error) {
        console.error("Error in sendListingRejectedNotification:", error);
      }
    }
  });

exports.sendNewRequestNotification = functions.firestore
  .document("requests/{requestId}")
  .onCreate(async (snap, context) => {
    const newRequest = snap.data();
    const requestId = context.params.requestId;

    try {
      const userDoc = await admin.firestore().collection("users").doc(newRequest.userID).get();
      if (!userDoc.exists) {
        console.log("User not found:", newRequest.userID);
        return;
      }
      const userData = userDoc.data();

      if (!userData.fcmToken) {
        console.log("FCM token not found for user:", newRequest.userID);
        return;
      }

      const notificationPromises = [
        // Kullanıcıya bildirim kaydı
        admin.firestore().collection("users").document(newRequest.userID)
          .collection("notifications")
          .add({
            title: "Talep Oluşturuldu",
            message: "Talebiniz başarıyla oluşturuldu. Eşleşen ilanlar olduğunda bilgilendirileceksiniz.",
            type: "newRequest",
            relatedID: requestId,
            timestamp: admin.firestore.FieldValue.serverTimestamp(),
            isRead: false,
          }),
        // FCM bildirimi
        admin.messaging().send({
          notification: {
            title: "Talep Oluşturuldu",
            body: "Talebiniz başarıyla oluşturuldu. Eşleşen ilanlar olduğunda bilgilendirileceksiniz.",
          },
          data: {
            type: "request",
            requestId: requestId,
          },
          token: userData.fcmToken,
          ...notificationSound,
        }),
      ];

      await Promise.all(notificationPromises);
      console.log("New request notifications sent successfully");
    } catch (error) {
      console.error("Error in sendNewRequestNotification:", error);
    }
  });
