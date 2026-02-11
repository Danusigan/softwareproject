package com.example.Software.project.Backend.Repository;

public class UserRepository {

<<<<<<< Updated upstream
<<<<<<< Updated upstream
<<<<<<< Updated upstream
<<<<<<< Updated upstream
<<<<<<< Updated upstream
<<<<<<< Updated upstream
<<<<<<< Updated upstream
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByUsername(String username);
    Optional<User> findByUsertype(String usertype);
    Optional<User> findByEmail(String email);
    
    // Add explicit method to find by the username field (which is actually userID)
    // This should work since the field name is 'username' in the entity
    default Optional<User> findByUserID(String userID) {
        return findByUsername(userID);
    }
}
=======
}
>>>>>>> Stashed changes
=======
}
>>>>>>> Stashed changes
=======
}
>>>>>>> Stashed changes
=======
}
>>>>>>> Stashed changes
=======
}
>>>>>>> Stashed changes
=======
}
>>>>>>> Stashed changes
=======
}
>>>>>>> Stashed changes
